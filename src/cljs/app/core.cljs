(ns app.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            [app.graph-panel :as graph-panel]
            ))

(defn run []
  (rdom/render
   [graph-panel/draw-graph]
   (js/document.getElementById "root")))

;; Exported methods
; On init
(defn ^:export init [] (run))

; On reload
(defn ^:dev/after-load start [] (run))

;; optional
;(defn ^:dev/before-load stop []
;  (js/console.log "stop"))a
