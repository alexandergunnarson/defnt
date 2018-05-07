(ns quantum.untyped.core.type.predicates
  (:refer-clojure :exclude
    [any? boolean? ident? qualified-keyword? simple-symbol?])
  (:require
    [clojure.core   :as core]
#?(:clj
    [clojure.future :as fcore])
    [quantum.untyped.core.vars
      :refer [defalias]]))

;; The reason we use `resolve` and `eval` here is that currently we need to prefer built-in impls
;; where possible in order to leverage their generators

#?(:clj  (eval `(defalias ~(if (resolve `fcore/any?)
                               `fcore/any?
                               `core/any?)))
   :cljs (defalias core/any?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/boolean?)
                               `fcore/boolean?
                               `core/boolean?)))
   :cljs (defalias core/boolean?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/ident?)
                               `fcore/ident?
                               `core/ident?)))
   :cljs (defalias core/ident?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/qualified-keyword?)
                               `fcore/qualified-keyword?
                               `core/qualified-keyword?)))
   :cljs (defalias core/qualified-keyword?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/simple-symbol?)
                               `fcore/simple-symbol?
                               `core/simple-symbol?)))
   :cljs (defalias core/simple-symbol?))

(def val? some?)
