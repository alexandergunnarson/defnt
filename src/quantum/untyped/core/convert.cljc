(ns quantum.untyped.core.convert)

;; NOTE: Simplified from the Quantum original
(defn >keyword [x]
  (cond (keyword? x) x
        (symbol?  x) (keyword (namespace x) (name x))
        :else        (-> x str keyword)))
