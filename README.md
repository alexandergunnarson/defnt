# `defnt`
Where `defn` meets [`clojure.spec`](https://clojure.org/about/spec) and a gradual-typing baby is born.

Extracted from [`quantum`](https://github.com/alexandergunnarson/quantum), but maintained in this separate repository until `quantum` reaches a stable state.

Rationale and Summary
---

Fundamentally, **it provides spec information in context rather than drastically increasing cognitive overhead and code duplication via `s/fdef`-style decontextualization.** (There are other features and benefits that will be approached as development progresses.)

`clojure.spec` is a great leap forward for Clojure. It is difficult to overstate the value that it delivers via the expressive and composable data specifications it supports, and the corresponding generative tests they in turn yield out of the box. But, as ever, there is room for improvement. It is a perfectly defensible design decision for the creators of `clojure.spec` to have decoupled specs from the implementation they specify. However, the position `defnt` takes is that there is much greater value in colocating the spec with the spec'ed. This approach yields the following benefits:

- It is much more terse than `fdef` + `defn`
- It is much easier to follow
- It is much easier to debug and maintain
- As such, it is much better about encouraging and facilitating the spec'ing of functions.

At the moment, it has only been tested with Clojure 1.8 and 1.9, but it should be trivial to test with ClojureScript in a coming release, as there is no platform-specific code.

Related Works
---

- [Orchestra](https://github.com/jeaye/orchestra)'s `defn-spec` was developed independently, but surprisingly yields some very similar ideas with respect to `defns` (see below) and even a similar interface for specs on function arguments.
- [Spectrum](https://github.com/arohner/spectrum) was also developed independently, and shares `defnt`'s idea of performing spec conformance checks at compile time (though it aims exclusively for "there-exists" checks via generative testing; `defnt` also provides as many compile-time "for-all" proofs as it can before falling back to compile-time generative testing and/or runtime spec checks).

Usage
---

There are two ways of defining typed/spec'ed functions using the code in this repository: 1) using the `defnt` macro, and 2) using its implementationally simpler but less powerful sibling, the `defns` macro (`defnt` is short for `defn`, `t`yped; `defns` is short for `defn`, `s`pec'ed). For now, since `defnt` is not yet fully implemented and since its interface is identical except for the addition of type/spec dispatch, we will approach only `defns`.

To take a reasonably simple example:

```clojure
(require '[quantum.core.defnt :refer [defns]])

(defns abcde 
  [a pos-int?, b (s/and double? #(> % 3)) 
   | (> b a)
   > (s/and map? #(= (:a %) a))]
  {:a a :b (+ b 8)})
```

Deconstructed, the above code defines a function `abcde` with only one overload, such that:
- The overload takes two parameters, `a` and `b`
- `a` must satisfy `pos-int?`
- `b` must satisfy `(s/and double? #(> % 3))`
- `(> b a)` must hold true
  - `|` defines an overload's precondition similarly to `:pre` (but implementationally the precondition becomes part of `fdef` and does not use `:pre` in any way)
  - Preconditions are optional
- The return value from the overload must satisfy `(s/and map? #(= (:a %) a))`
  - `>` defines an overload's postcondition similarly to `:post` (but like `|`, implementationally the precondition becomes part of `fdef` and does not use `:post` in any way)
  - Postconditions are optional

The above `defns` code generates the following:

```clojure
(s/fdef abcde
  :args (s/or :arity-2
                (s/and
                  (s/cat
                   :a (qs/with (fn [a] a) pos-int?)
                   :b (qs/with (fn [b] b) (s/and double? #(> % 3))))
                  (fn [{a :a b :b}] (> b a))))
  :fn   (qs/with-gen-spec
          (fn [{ret# :ret}] ret#)
          (fn [{[arity-kind# args#] :args}]
           (case arity-kind#
             :arity-2
             (let [{a :a b :b} args#]
               (s/spec (s/and map? #(= (:a %) a))))))))

(defn abcde ([a b] {:a a, :b (+ b 8)}))
```

where `qs/with` and `qs/with-gen-spec` are low-complexity, few-LOC functions in `quantum.untyped.core.spec` that assist in spec auditability and data flow.

Advanced Usage
---

Note that spec'ing destructurings is also possible. Take the more complex example below:

```clojure
(defns fghij "Some documentation" {:whatever-metadata "abc"}
  ([a number? > number?] (inc a))
  ([a number?, b number?
    | (> a b)
    > (s/and number? #(> % a) #(> % b))] (+ a b))
  ([a string?
    b boolean?
    {:as   c
     :keys [ca keyword? cb string?]
     {:as cc
      {:as   cca
       :keys [ccaa keyword?]
       [[ccabaa some? {:as ccabab :keys [ccababa some?]} some?] some? ccabb some? :as ccab]
       [:ccab seq?]}
      [:cca map?]}
     [:cc map?]}
    #(-> % count (= 3))
    [da double? & db seq? :as d] sequential?
    [ea symbol?] vector?
    & [fa string? :as f] seq?
    | (and (> a b) (contains? c a)
           a b c ca cb cc cca ccaa ccab ccabaa ccabab ccababa ccabb d da db ea f fa)
    > number?] 0))
```

which expands to:

```clojure
(s/fdef fghijk
  :args
    (s/or
      :arity-1 (s/cat :a (s/with (fn [a] a) number?))
      :arity-2 (s/and (s/cat :a (s/with (fn [a] a) number?)
                             :b (s/with (fn [b] b) number?))
                      (fn [{a :a b :b}] (> a b)))
      :arity-varargs
        (s/and
          (s/cat
            :a      (qs/with (fn [a] a) string?)
            :b      (qs/with (fn [b] b) boolean?)
            :c      (s/and
                      (qs/with (fn [c]                                             c)        #(-> % count (= 3)))
                      (qs/with (fn [{:keys [ca]}]                                  ca)       keyword?)
                      (qs/with (fn [{:keys [cb]}]                                  cb)       string?)
                      (qs/with (fn [{cc :cc}]                                      cc)       map?)
                      (qs/with (fn [{{cca :cca} :cc}]                              cca)      map?)
                      (qs/with (fn [{{{:keys [ccaa]} :cca} :cc}]                   ccaa)     keyword?)
                      (qs/with (fn [{{{ccab :ccab} :cca} :cc}]                     ccab)     seq?)
                      (qs/with (fn [{{{[as#] :ccab} :cca} :cc}]                    as#)      some?)
                      (qs/with (fn [{{{[[ccabaa]] :ccab} :cca} :cc}]               ccabaa)   some?)
                      (qs/with (fn [{{{[[_# ccabab]] :ccab} :cca} :cc}]            ccabab)   some?)
                      (qs/with (fn [{{{[[_# {:keys [ccababa]}]] :ccab} :cca} :cc}] ccababa)) some?
                      (qs/with (fn [{{{[_# ccabb] :ccab} :cca} :cc}]               ccabb)    some?))
            :d      (s/and
                      (qs/with (fn [d]         d)   sequential?)
                      (qs/with (fn [[da]]      da)  double?)
                      (qs/with (fn [[_# & db]] db)) seq?)
            :arg-4# (s/qand
                      (qs/with (fn [as#]  as#) vector?)
                      (qs/with (fn [[ea]] ea)) symbol?)
            :f      (s/and
                      (qs/with (fn [f]    f)  seq?)
                      (qs/with (fn [[fa]] fa) string?)))
          (fn [{a :a
                b :b
                {:as c
                 :keys [ca cb]
                 {:as cc
                  {:as cca
                   :keys [ccaa]
                   [[ccabaa {:as ccabab :keys [ccababa]}] ccabb :as ccab] :ccab} :cca} :cc} :c
                [da & db :as d] :d
                [ea] :arg-4#
                [fa :as f] :f}]
            (and (> a b) (contains? c a)
                 a b c ca cb cc cca ccaa ccab ccabaa ccabab ccababa ccabb d da db ea f fa))))
   :fn
     (qs/with-gen-spec (fn [{:keys [ret]}] ret)
       (fn [{[arity-kind# args#] :args}]
         (case arity-kind#
           :arity-1
             (let [{a :a} args#] (s/spec number?))
           :arity-2
             (let [{a :a b :b} args#] (s/spec (s/and number? #(> % a) #(> % b))))
           :arity-3
             (let [{a :a
                    b :b
                    {:as c
                     :keys [ca cb]
                     {:as cc
                      {:as cca
                       :keys [ccaa]
                       [[ccabaa {:as ccabab :keys [ccababa]}] ccabb :as ccab] :ccab} :cca} :cc} :c
                    [da & db :as d] :d
                    [ea] :arg-4#
                    [fa :as f] :f} args#] (s/spec number?))))))
                    
(defn fghij
  "Documentation"
  {:whatever-metadata "abc"}
  ([a] (inc a))
  ([a b] (+ a b))
  ([a b
    {:as c
     :keys [ca cb]
     {:as cc
      {:as cca
       :keys [ccaa]
       [[ccabaa {:as ccabab :keys [ccababa]}] ccabb :as ccab] :ccab} :cca} :cc}
    [da & db :as d]
    [ea]
    &
    [fa :as f]]
   0))
```

Copyright and License
---
*Copyright © 2018 Alex Gunnarson*

*Distributed under the Creative Commons Attribution-ShareAlike 3.0 US (CC-SA) license.*

**For normal people who don't speak legalese, this means:**

* You **can** modify the code
* You **can** distribute the code
* You **can** use the code for commercial purposes

But:

* You **have to** give credit / attribute the code to the author (Alex Gunnarson)
* You **have to** state the name of the author (Alex Gunnarson) and the title of this project in the attribution
* You **have to** say in the attribution that you modified the code if you did

Pretty easy, common-sense, decent stuff! Thanks :)

*For more information, see [tldrlegal's summary](https://tldrlegal.com/license/creative-commons-attribution-share-alike-(cc-sa)) of the CC-SA license.*
