(defproject web-service-browser "0.1.0-SNAPSHOT"
  :description      "A swing app for browsing web services"
  :min-lein-version "2.0.0"
  :url              "https://github.com/glucero/web_service_browser"
  :dependencies     [[org.clojure/clojure "1.4.0"]
                     [seesaw              "1.4.2"]
                     [clj-http            "0.6.5"]
                     [hiccup              "1.0.2"]]
  :main             web-service-browser.core)

