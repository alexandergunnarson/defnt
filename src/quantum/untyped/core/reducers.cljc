(ns quantum.untyped.core.reducers)

(defn reducei
  "`reduce`, indexed."
  [f init xs]
  (let [f' (let [*i (volatile! -1)]
              (fn ([ret x]
                    (f ret x (vreset! *i (unchecked-inc (long @*i)))))))]
    (reduce f' init xs)))
