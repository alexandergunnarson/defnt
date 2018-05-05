(ns quantum.untyped.core.spec
  (:require
    [clojure.spec.alpha     :as s]
    [clojure.spec.gen.alpha :as gen]))

(defn validate [spec x]
  (let [conformed (s/conform spec x)]
    (if (s/invalid? conformed)
        (let [ed (merge (assoc (s/explain-data* spec [] [] [] x)
                          ::s/failure :assertion-failed))]
          (throw (ex-info
                   (str "Spec assertion failed\n" (with-out-str (s/explain-out ed)))
                   ed))))))

;; NOTE: modified from the Quantum original
#?(:clj (defmacro with [extract-f spec] `(s/nonconforming (and (s/conformer ~extract-f) ~spec))))

;; NOTE: modified from the Quantum original
(defn with-gen-spec-impl
  "Do not call this directly; use 'with-gen-spec'."
  [extract-f extract-f|form gen-spec gen-spec|form]
  (let [form `(with-gen-spec ~extract-f|form ~gen-spec|form)
        gen-spec (fn [x] (let [spec (gen-spec x)
                               desc (s/describe spec)
                               desc (if (= desc ::s/unknown)
                                        (list 'some-generated-spec gen-spec|form)
                                        desc)]
                           (with extract-f (@#'s/spec-impl desc spec nil nil))))]
    (if (fn? gen-spec)
        (reify
          s/Specize
            (s/specize*  [this] this)
            (s/specize*  [this _] this)
          s/Spec
            (s/conform*  [_ x] (s/conform* (gen-spec x) x))
            (s/unform*   [_ x] (s/unform* (gen-spec x) x))
            (s/explain*  [_ path via in x] (s/explain* (gen-spec x) path via in x))
            (s/gen*      [_ _ _ _] (gen/gen-for-pred gen-spec))
            (s/with-gen* [_ gen-fn'] (throw (ex-info "TODO" {})))
            (s/describe* [_] form))
        (throw (ex-info "`wrap-spec` may only be called on fns" {:input gen-spec})))))

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

