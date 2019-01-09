(ns {{namespace}}
  (:require
   ["react-blessed" :as react-blessed]
   [blessed :as blessed]
   [cljs.nodejs :as nodejs]
   ;; TODO: Which of these do we need?
   [{{main-ns}}.debug.view :as debug-view]
   [{{main-ns}}.keys :as keys]
   [{{main-ns}}.subs]
   [{{main-ns}}.events]
   [{{main-ns}}.view :as view]
   [fs :as fs]
   [mount.core :refer [defstate] :as mount]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [tty :as tty]))

(mount/in-cljc-mode)

(defstate tty-fd :start (fs/openSync "/dev/tty" "r+"))
(defstate program :start (blessed/program #js
                                          {:input (tty/ReadStream @tty-fd)
                                           :output (tty/WriteStream @tty-fd)}))

(defstate screen :start (doto
                         (blessed/screen #js {:program @program
                                              :autoPadding true
                                              :smartCSR true
                                              :title "{{name}}"})
                         keys/setup))

(defonce render (react-blessed/createBlessedRenderer blessed))

(defn log-fn
  [& args]
  (swap! debug-view/logger conj (clojure.string/join " " args)))

(defn load
  []
  (-> (r/reactify-component view/root)
      (r/create-element #js {})
      (render @screen)))

(defn -main
  []
  (mount/start)
  (rf/dispatch-sync [:init])
  (load))

(set! (.-log js/console) log-fn)
(re-frame.loggers/set-loggers! {:log log-fn
                                :warn log-fn})

(set! *main-cli-fn* -main)
