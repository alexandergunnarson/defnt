(ns quantum.untyped.core.vars
  (:require
    [quantum.untyped.core.form.evaluate
      :refer [case-env]]))

#?(:clj
(defn defalias* [^clojure.lang.Var orig-var ns-name- var-name]
  (let [;; to avoid warnings
        var-name' (with-meta var-name (-> orig-var meta (select-keys [:dynamic])))
        ^clojure.lang.Var var-
          (if (.hasRoot orig-var)
              (intern ns-name- var-name' @orig-var)
              (intern ns-name- var-name'))]
    ;; because this doesn't always get set correctly
    (cond-> var-
      (.isDynamic orig-var)
      (doto (.setDynamic))))))

#?(:clj
(defmacro defalias
  "Defines an alias for a var: a new var with the same root binding (if
  any) and similar metadata. The metadata of the alias is its initial
  metadata (as provided by def) merged into the metadata of the original."
  {:attribution  'clojure.contrib.def/defalias
   :contributors ["Alex Gunnarson"]}
  ([orig]
    `(defalias ~(symbol (name orig)) ~orig))
  ([name orig]
    `(doto ~(case-env
               :clj  `(defalias* (var ~orig) '~(ns-name *ns*) '~name)
               :cljs `(def ~name (-> ~orig var deref)))
            (alter-meta! merge (meta (var ~orig)))))
  ([name orig doc]
     (list `defalias (with-meta name (assoc (meta name) :doc doc)) orig))))
