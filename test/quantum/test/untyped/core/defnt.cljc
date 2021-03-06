(ns quantum.test.untyped.core.defnt
  (:refer-clojure :exclude
    [boolean? double? pos-int?])
  (:require
    [clojure.spec.alpha         :as s]
    [clojure.spec.gen.alpha     :as gen]
    [clojure.spec.test.alpha    :as stest]
    [clojure.test.check.clojure-test
      :refer [defspec]]
    [quantum.untyped.core.defnt :as this]
    [quantum.untyped.core.spec  :as us]
    [quantum.untyped.core.test
      :refer [defspec-test]]
    [quantum.untyped.core.type.predicates
      :refer [boolean? double? pos-int?]]))

;; Implicit compilation tests
(this/defns abcde "Documentation" {:metadata "abc"}
  ([a number? > number?] (inc a))
  ([a pos-int?, b pos-int?
    | (> a b)
    > (s/and number? #(> % a) #(> % b))] (+ a b))
  ([a #{"a" "b" "c"}
    b boolean?
    {:as   c
     :keys [ca keyword? cb string?]
     {:as cc
      {:as   cca
       :keys [ccaa keyword?]
       [[ccabaa some? {:as ccabab :keys [ccababa some?]} some?] some? ccabb some? & ccabc some? :as ccab]
       [:ccab seq?]}
      [:cca map?]}
     [:cc map?]}
    #(-> % count (= 3))
    [da double? & db seq? :as d] sequential?
    [ea symbol?] ^:gen? (s/coll-of symbol? :kind vector?)
    & [fa #{"a" "b" "c"} :as f] seq?
    | (and (> da 50) (contains? c a)
           a b c ca cb cc cca ccaa ccab ccabaa ccabab ccababa ccabb ccabc d da db ea f fa)
    > number?] 0))

#?(:clj
(defmacro defns-test [sym & args]
  `(do (this/defns ~sym ~@args)
       (defspec-test ~(symbol (str "test|" sym)) (symbol (str (ns-name *ns*)) ~(str sym))))))

(defns-test basic [a number? > number?] (rand))

(defns-test equality [a number? > #(= % a)] a)

(defns-test pre-post [a number? | (> a 3) > #(> % 4)] (inc a))

(defns-test gen|seq|0 [[a number? b number? :as b] ^:gen? (s/tuple double? double?)])

(defns-test gen|seq|1
  [[a number? b number? :as b] ^:gen? (s/nonconforming (s/cat :a double? :b double?))])

(defns-test underscore-binding [a number? _ string?] a)

(defns-test underscore-spec|sym [a _ b string?] a)

(defns-test underscore-spec|seq [[a string?] _ b string?] a)

(defns-test underscore-spec|map [{:keys [a string?]} _ b string?] a)

(defns-test underscore-binding+spec [a _ _ #{""} > #{""}] _)

;; TODO assert that the below 2 things are equivalent

#_(this/defns abcde "Documentation" {:metadata "fhgjik"}
  ([a number? > number?] (inc a))
  ([a pos-int?, b pos-int?
    | (> a b)
    > (s/and number? #(> % a) #(> % b))] (+ a b))
  ([a #{"a" "b" "c"}
    b boolean?
    {:as   c
     :keys [ca keyword? cb string?]
     {:as cc
      {:as   cca
       :keys [ccaa keyword?]
       [[ccabaa some? {:as ccabab :keys [ccababa some?]} some?] some? ccabb some? & ccabc some? :as ccab]
       [:ccab seq?]}
      [:cca map?]}
     [:cc map?]}
    #(-> % count (= 3))
    [da double? & db seq? :as d] sequential?
    [ea symbol?] ^:gen? (s/coll-of symbol? :kind vector?)
    & [fa #{"a" "b" "c"} :as f] seq?
    | (and (> da 50) (contains? c a)
           a b c ca cb cc cca ccaa ccab ccabaa ccabab ccababa ccabb ccabc d da db ea f fa)
    > number?] 0))

#_(s/fdef abcde
  :args
    (s/or
      :arity-1 (s/cat :a number?)
      :arity-2 (s/and (s/cat :a pos-int?
                             :b pos-int?)
                      (fn [{a :a b :b}] (> a b)))
      :arity-varargs
        (s/and
          (s/cat
            :a      #{"a" "b" "c"}
            :b      boolean?
            :c      (this/map-destructure #(-> % count (= 3))
                      {:ca keyword?
                       :cb string?
                       :cc (this/map-destructure map?
                             {:cca (this/map-destructure map?
                                     {:ccaa keyword?
                                      :ccab (this/seq-destructure seq?
                                              [:arg-0 (this/seq-destructure some?
                                                        [:ccabaa some?
                                                         :ccabab (this/map-destructure some? {:ccababa some?})])
                                               :ccabb some?]
                                              [:ccabc some?])})})})
            :d      (this/seq-destructure sequential? [:da double?] [:db seq?])
            :arg-4# (this/seq-destructure ^{:gen? true} (s/coll-of symbol? :kind vector?) [:ea symbol?] )
            :f      (this/seq-destructure seq? [:fa #{"a" "b" "c"}]))
          (fn [{a :a
                b :b
                {:as c
                 :keys [ca cb]
                 {:as cc
                  {:as cca
                   :keys [ccaa]
                   [[ccabaa {:as ccabab :keys [ccababa]}] ccabb & ccabc :as ccab] :ccab} :cca} :cc} :c
                [da & db :as d] :d
                [ea] :arg-4#
                [fa :as f] :f :as X}]
            (and (> da 50) (= a fa)
                 a b c ca cb cc cca ccaa ccab ccabaa ccabab ccababa ccabb ccabc d da db ea f fa))))
   :fn
     (us/with-gen-spec (fn [{ret# :ret}] ret#)
       (fn [{[arity-kind# args#] :args}]
         (case arity-kind#
           :arity-1
             (let [{a :a} args#] (s/spec number?))
           :arity-2
             (let [{a :a b :b} args#] (s/spec (s/and number? #(> % a) #(> % b))))
           :arity-varargs
             (let [{a :a
                    b :b
                    {:as c
                     :keys [ca cb]
                     {:as cc
                      {:as cca
                       :keys [ccaa]
                       [[ccabaa {:as ccabab :keys [ccababa]}] ccabb & ccabc :as ccab] :ccab} :cca} :cc} :c
                    [da & db :as d] :d
                    [ea] :arg-4#
                    [fa :as f] :f} args#] (s/spec number?))))))

#_(defn abcde "Documentation" {:metadata "abc"}
  ([a] (inc a))
  ([a b] (+ a b))
  ([a b
    {:as c,
     :keys [ca cb],
     {:as cc,
      {:as cca,
       :keys [ccaa],
       [[ccabaa {:as ccabab, :keys [ccababa]}] ccabb & ccabc :as ccab] :ccab} :cca} :cc}
    [da & db :as d]
    [ea]
    &
    [fa :as f]]
   0))
