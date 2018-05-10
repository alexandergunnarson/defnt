(def deps|clj-1-8
 '[[clojure-future-spec    "1.9.0-beta4"]
   [frankiesardo/linked    "1.2.9"]
   [org.clojure/clojure    "1.8.0"]])

(def deps|clj-1-9
 '[[clojure-future-spec    "1.9.0-beta4"]
   [frankiesardo/linked    "1.2.9"]
   [org.clojure/clojure    "1.9.0"]
   [org.clojure/spec.alpha "0.1.143"]])

(defproject quantum/defnt "0.2.0"
  :description  "Where `defn` meets `clojure.spec` and a gradual-typing baby is born."
  :url          "https://github.com/alexandergunnarson/defnt"
  :license      {:name         "Creative Commons Attribution-ShareAlike 3.0 US (CC-SA)"
                 :url          "https://creativecommons.org/licenses/by-sa/3.0/us/"
                 :distribution :repo}
  :profiles
    {:dev||test {:global-vars {*warn-on-reflection* true
                               *unchecked-math*     :warn-on-boxed}}
     :dev       [:dev||test
                 {:dependencies ~(conj deps|clj-1-9
                                   '[expound                "0.6.0"]
                                   '[orchestra              "2017.11.12-1"]
                                   '[org.clojure/test.check "0.9.0"])}]
     :test      [:dev||test]
     :clj-1-8   {:dependencies ~(conj deps|clj-1-8
                                  '[org.clojure/test.check "0.9.0"])}
     :clj-1-9   {:dependencies ~(conj deps|clj-1-9
                                  '[org.clojure/test.check "0.9.0"])}})

