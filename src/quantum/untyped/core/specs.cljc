(ns quantum.untyped.core.specs
  (:refer-clojure :exclude [any? simple-symbol?])
  (:require
    [clojure.set        :as set]
    [clojure.spec.alpha :as s]
    [quantum.untyped.core.fn
      :refer [fn1 fnl]]
    [quantum.untyped.core.type.predicates
      :refer [any? simple-symbol? val?]]))

;;;; GENERAL

(s/def :quantum.core.specs/meta map?)

;; map destructuring

(s/def :quantum.core.specs/or (s/map-of simple-symbol? any?))

;; defn, defn-, fn

(s/def :quantum.core.specs/fn|name simple-symbol?)

(s/def :quantum.core.specs/docstring string?)

(s/def :quantum.core.specs/pre-meta  (s/? :quantum.core.specs/meta))
(s/def :quantum.core.specs/post-meta (s/? :quantum.core.specs/meta))

(s/def :quantum.core.specs/fn|unique-doc
  #(->> [(:quantum.core.specs/docstring %)
         (-> % :quantum.core.specs/fn|name meta :doc)
         (-> % :quantum.core.specs/pre-meta     :doc)
         (-> % :quantum.core.specs/post-meta    :doc)]
        (filter val?)
        count
        ((fn [x] (<= x 1)))))

(s/def :quantum.core.specs/fn|unique-meta
  #(empty? (set/intersection
             (-> % :quantum.core.specs/fn|name meta keys set)
             (-> % :quantum.core.specs/pre-meta     keys set)
             (-> % :quantum.core.specs/post-meta    keys set))))

(s/def :quantum.core.specs/fn|aggregate-meta
  (s/conformer
    (fn [{:keys [:quantum.core.specs/fn|name :quantum.core.specs/docstring
                 :quantum.core.specs/pre-meta :quantum.core.specs/post-meta] :as m}]
      (-> m
          (dissoc :quantum.core.specs/docstring
                  :quantum.core.specs/pre-meta
                  :quantum.core.specs/post-meta)
          (cond-> fn|name
            (update :quantum.core.specs/fn|name with-meta
              (-> (merge (meta fn|name) pre-meta post-meta) ; TODO use `merge-unique` instead of `:quantum.core.specs/defn|unique-meta`
                  (cond-> docstring (assoc :doc docstring)))))))))

(defn fn-like|postchecks|gen [overloads-ident]
  (s/and (s/conformer
           (fn [v]
             (let [[overloads-k overloads-v] (get v overloads-ident)
                   overloads
                    (-> (case overloads-k
                          :overload-1 {:overloads [overloads-v]}
                          :overload-n overloads-v)
                        (update :overloads
                          (fnl mapv
                            (fn1 update :body
                              (fn [[k v]]
                                (case k
                                  :body         {:body v}
                                  :prepost+body v))))))]
               (assoc v :quantum.core.specs/post-meta (:quantum.core.specs/post-meta overloads)
                        overloads-ident               (get overloads :overloads)))))
         :quantum.core.specs/fn|unique-doc
         :quantum.core.specs/fn|unique-meta
         ;; TODO validate metadata like return value etc.
         :quantum.core.specs/fn|aggregate-meta))

(s/def :quantum.core.specs/fn|postchecks (fn-like|postchecks|gen :quantum.core.specs/fn|overloads))
