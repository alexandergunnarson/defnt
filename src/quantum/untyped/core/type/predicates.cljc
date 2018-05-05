(ns quantum.untyped.core.type.predicates
  (:refer-clojure :exclude
    [any? boolean? ident? qualified-keyword? simple-symbol?]))

(defn any?
  "Returns true given any argument."
  [x] true)

(defn boolean? [x] #?(:clj  (instance? Boolean x)
                      :cljs (or (true? x) (false? x))))

(defn ident?
  "Return true if x is a symbol or keyword"
  [x] (or (keyword? x) (symbol? x)))

(defn qualified-keyword?
  "Return true if x is a keyword with a namespace"
  [x] (boolean (and (keyword? x) (namespace x) true)))

(defn simple-symbol?
  "Return true if x is a symbol without a namespace"
  [x] (and (symbol? x) (nil? (namespace x))))

(def val? some?)
