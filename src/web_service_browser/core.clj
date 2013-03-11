(ns web-service-browser.core
  (:require [clj-http.client :as http]
            [cheshire.core   :as json]
            [clojure.string  :as s])

  (:import  javax.swing.event.HyperlinkEvent
            javax.swing.event.HyperlinkEvent$EventType
            java.net.URL)

  (:use     seesaw.core ; <3
            seesaw.font
            hiccup.core))

(declare open)
(declare login!)

(def ^:const _:_ " : ")
(def ^:dynamic *ssbe-user* "")
(def ^:dynamic *ssbe-pass* "")
(def ^:dynamic *ssbe-home* "")

(defn ssbe-http
  [method url & [options]] ; add :body "" for methods :put and :post
  (let [uri (URL. url)]
    (:body (http/request
      (merge options {:method          method ; :get, :put, :post
                      :scheme          :https
                      :server-name     (.getHost uri)
                      :server-port     443
                      :uri             (.getPath uri)
                      :query-string    (.getQuery uri)
                      :digest-auth     [*ssbe-user* *ssbe-pass*]
                      :conn-timeout    5000
                      :socket-timeout  5000
                      :client-params   {"http.useragent" "web-service-browser"}
                      :throw-exception false
                      :accept          :json
                      :content-type    :json
                      :as              :json})))))

(def app-window
  (frame :title        "Web Service Browser"
         :content      ""
         :minimum-size [900 :by 600]))

(def main-frame
  ; to give us a 'web browser' effect, we do 3 things
  ;
  ; 1. set this frame's content-type to 'text/html'
  ; 2. convert our json response to an html list with hyperlinks
  ; 3. set a mouse click listener for the frame that replaces the frame's
  ;    content with the result of a web service request
  (let [pane (editor-pane :id           :editor
                          :text         ""
                          :editable?    false
                          :content-type "text/html")]
    (listen pane :hyperlink (fn [event]
                              (when (= HyperlinkEvent$EventType/ACTIVATED (.getEventType event))
                                (open (.getDescription event)))))
    pane))

(def panel-pane
  (let [home-button  (button :text "home")   ; core.ssbe/service_descriptors
        login-button (button :text "login")] ; relaunch login process
    (listen home-button  :action (fn [event] (open *ssbe-home*)))
    (listen login-button :action (fn [event] (login!)))
    (horizontal-panel :items [login-button home-button])))

(defn string->link
  ([href]      (string->link href href))
  ([name href] [:a {:href href} name]))

(defn hashmap->list
  ([hashmap]                   (hashmap->list hashmap :ul :span _:_))
  ([hashmap list-type]         (hashmap->list hashmap list-type :span _:_))
  ([hashmap list-type wrapper] (hashmap->list hashmap list-type wrapper _:_))
  ([hashmap list-type wrapper separator]
  [list-type {:style "list-style-type: none;"}
    (for [[key val] hashmap]
      [:li [wrapper   ; :key-name : value or link
        (str (name key) separator) (if (map? val) ; create nested lists
                                     (hashmap->list val list-type wrapper separator)
                                     (if (re-matches #"^.*href$" (name key))
                                       (string->link val) ; make links for hrefs
                                       val))]])]))

(defn open
  [url]
  (let [response (ssbe-http :get url)]
    (config! main-frame :text (html (if-let [items (:items response)]
                                      (map hashmap->list items)
                                      (hashmap->list response)))))
  (config! app-window :content (border-panel :north  panel-pane
                                             :center (scrollable main-frame)
                                             :vgap   5
                                             :hgap   5
                                             :border 5))
  (scroll! main-frame :to :top))

(defn login! []
  (show! (pack!
    (dialog :content (grid-panel :columns 1
                                 :items   ["username" (text :id :username)
                                           "password" (password :id :password)
                                           "backend"  (text :id :backend)])
            :success-fn (fn [panel]
                          (let [{username :username
                                 password :password
                                 backend  :backend} (value panel)]
                            (alter-var-root (var *ssbe-user*) (constantly username))
                            (alter-var-root (var *ssbe-pass*) (constantly password))
                            (alter-var-root (var *ssbe-home*) (constantly (str "https://core." backend "/service_descriptors")))
                            (open *ssbe-home*)))))))

(defn -main []
  (native!)
  (login!)
  (-> app-window pack! show!))

