(ns quantum.untyped.core.spec
  (:refer-clojure :exclude [ident?])
  (:require
    [clojure.core           :as core]
    [clojure.spec.alpha     :as s]
    [clojure.spec.gen.alpha :as gen]
    [quantum.untyped.core.type.predicates
      :refer [ident?]]))

;; Implementation and interface modified from the Quantum original
(defn validate [spec x]
  (let [conformed (s/conform spec x)]
    (if (s/invalid? conformed)
        (let [ed (core/merge (assoc (s/explain-data* spec [] [] [] x)
                               ::s/failure :assertion-failed))]
          (throw (ex-info
                   (str "Spec assertion failed\n" (with-out-str (s/explain-out ed)))
                   ed)))
        conformed)))

(defn kv
  "Based on `s/map-spec-impl`"
  ([k->s #_(s/map-of any? specable?)] (kv k->s nil))
  ([k->s #_(s/map-of any? specable?) gen-fn #_(? fn?)]
    (let [id (java.util.UUID/randomUUID)
          k->s|desc (->> k->s
                         (map (fn [[k specable]]
                                [k (if (ident? specable) specable (s/describe specable))]))
                         (into {}))]
      (reify
        s/Specize
          (specize* [this] this)
          (specize* [this _] this)
        s/Spec
          (conform* [_ x]
            (reduce
              (fn [x' [k s]]
                (let [v  (get x' k)
                      cv (s/conform s v)]
                  (if (s/invalid? cv)
                      ::s/invalid
                      (if (identical? cv v)
                          x'
                          ;; TODO we might want to do `assoc?!`, depending
                          (assoc x' k cv)))))
              x
              k->s))
          (unform* [_ x]
            (reduce
              (fn [x' [k s]]
                (let [cv (get x' k)
                      v  (s/unform s cv)]
                  (if (identical? cv v)
                      x'
                      ;; TODO we might want to do `assoc?!`, depending
                      (assoc x' k v))))
              x
              k->s))
          (explain* [_ path via in x]
            (if-not ;; TODO we might want a more generalized `map?` predicate like `t/map?`, depending,
                    ;; which would affect more code below
                    (map? x)
              [{:path path :pred 'map? :val x :via via :in in}]
              ;; TODO use reducers?
              (->> k->s
                   (map (fn [[k s]]
                          (let [v (get x k)]
                            (when-not (s/valid? s v)
                              (@#'s/explain-1 (get k->s|desc k) s (conj path k) via (conj in k) v)))))
                   (filter some?)
                   (apply concat))))
          (gen* [_ overrides path rmap]
            (if gen-fn
                (gen-fn)
                (let [rmap (assoc rmap id (inc (core/or (get rmap id) 0)))
                      gen  (fn [[k s]]
                             (when-not (@#'s/recur-limit? rmap id path k)
                               [k (gen/delay (@#'s/gensub s overrides (conj path k) rmap k))]))
                      gens (->> k->s (map gen) (remove nil?) (into {}))]
                  (gen/bind (gen/choose 0 (count gens))
                            (fn [n]
                              (let [args (-> gens seq shuffle)]
                                (->> args
                                     (take n)
                                     (apply concat)
                                     (apply gen/hash-map))))))))
          (with-gen* [_ gen-fn'] (kv k->s gen-fn'))
          (describe* [_] `(kv ~k->s|desc))))))

(defn with-gen-spec-impl
  "Do not call this directly; use 'with-gen-spec'."
  [extract-f extract-f|form gen-spec gen-spec|form]
  (if (fn? gen-spec)
      (let [form      `(with-gen-spec ~extract-f|form ~gen-spec|form)
            gen-spec' (fn [x]
                        (let [spec (gen-spec x)
                              desc (s/describe spec)
                              desc (if (= desc ::s/unknown)
                                       (list 'some-generated-spec gen-spec|form)
                                       desc)]
                          (s/nonconforming (s/and (s/conformer extract-f)
                                                  (@#'s/spec-impl desc spec nil nil)))))]
        (reify
          s/Specize
            (s/specize*  [this] this)
            (s/specize*  [this _] this)
          s/Spec
            (s/conform*  [_ x] (s/conform* (gen-spec' x) x))
            (s/unform*   [_ x] (s/unform* (gen-spec' x) x))
            (s/explain*  [_ path via in x] (s/explain* (gen-spec' x) path via in x))
            (s/gen*      [_ _ _ _] (gen/gen-for-pred gen-spec))
            (s/with-gen* [_ _] (throw (ex-info "TODO" {})))
            (s/describe* [_] form)))
      (throw (ex-info "`wrap-spec` may only be called on fns" {:input gen-spec}))))

#?(:clj
(defmacro with-gen-spec
  "`gen-spec` : an fn that returns a spec based on the input.
   `extract-f`: extracts the piece of data from the input that the generated spec will validate.
   E.g.:
   (s/explain
     (s/with-gen-spec (fn [{:keys [a]}] a) (fn [{:keys [b]}] #(> % b)))
     {:a 1 :b 1})"
  [extract-f gen-spec]
  `(with-gen-spec-impl ~extract-f '~extract-f ~gen-spec '~gen-spec)))
