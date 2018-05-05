(defproject quantum/defnt "0.0.1"
  :description  "Where `defn` meets `clojure.spec` and a gradual-typing baby is born."
  :url          "https://github.com/alexandergunnarson/defnt"
  :license      {:name         "Creative Commons Attribution-ShareAlike 3.0 US (CC-SA)"
                 :url          "https://creativecommons.org/licenses/by-sa/3.0/us/"
                 :distribution :repo}
  :dependencies [[org.clojure/clojure "1.8.0"] ; or 1.9.0
                 [clojure-future-spec "1.9.0-beta4"]
                 [frankiesardo/linked "1.2.9"]]
  :profiles
    {:dev {:global-vars  {*warn-on-reflection* true
                          *unchecked-math*     :warn-on-boxed}
           :dependencies [[org.clojure/clojure    "1.9.0"]
                          [expound                "0.6.0"]
                          [orchestra              "2017.11.12-1"]
                          [org.clojure/spec.alpha "0.1.143"]
                          [org.clojure/test.check "0.9.0"]]}})

