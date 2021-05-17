; TODOs
; 1. Undo/redo
; 2. Add nodes
(ns app.graph-panel
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            [applied-science.js-interop :as j]
            ["react-flow-renderer" :default ReactFlow :refer [Background Controls ReactFlowProvider]]))
(def react-flow-pro (r/adapt-react-class ReactFlowProvider))
(def react-flow (r/adapt-react-class ReactFlow))
(def background (r/adapt-react-class Background))
(def controls (r/adapt-react-class Controls))

(defonce graph (r/atom []))

(defn- add-node [node]
  (swap! graph conj node))

(defn- make-edge [source target]
  {:id (str source "->" target) :source source :target target})

(defn- add-edge [source target]
  (swap! graph conj (make-edge source target)))

(defn- remove-node [node]
                                        ; TODO
                                        ;(swap! graph conj node)
  )

(defn- make-node [id position-x position-y]
  (let [!state (r/atom "")]
    {:id id
     :style {:fontSize 14
             :fontFamily "monospace"
             :wordBreak "break-word"
             :width 200}
     :data {:label (r/as-element
                    [:div id [:br]
                     [:input
                      {:on-change (fn [e]
                                    (reset! !state (-> e .-target .-value)))}]])}

     :sourcePosition "right"
     :targetPosition "left"
     :position {:x position-x :y position-y}}))

(defn create-elements []
  (r/atom [(make-node "1" 50 200)
           (make-node "2" 300 100)
           (make-node "3" 300 300)
           (make-node "4" 550 200)
           (make-node "6" 50 500)
           (make-node "7" 550 500)
           (make-edge "1" "2")
           (make-edge "2" "4")
           (make-edge "3" "4")]))

(defn on-element-remove
  "Remove given node from graph."
  [elements event]
  (let [id (j/get-in event [0 :id])]
    (js/console.log (str "Delete " id))
                                        ; Remove from list
    (swap! elements (fn [elems]
                      (remove (fn [x*]
                                (or
                                 ; Remove the node
                                 (= (:id x*) id)
                                 ; Remove edges to or from
                                 (= (:source x*) id)
                                 (= (:target x*) id)))
                              elems)))))

(defn- flow-panel []
  (let [!text (r/atom "Text")
        elements (create-elements)]
    (r/create-class
     {:reagent-render (fn []
                        [:div
                         [react-flow-pro
                          [react-flow
                           {:default-position [10 10]
                            :style {:width "500"
                                    :height "500"
                                    :position "absolute"
                                    :top "100"
                                    :left "100"
                                    :background "white"}
                            :snap-to-grid true
                            :snap-grid [15 15]
                            :elements @elements
                            :on-connect #(let [source (.-source %)
                                               target (.-target %)]
                                           (js/console.log %)
                                           (swap! elements conj {:id (str/join "->" [source target]) :source source :target target}))
                            :on-elements-remove (fn [event] (on-element-remove elements event))
                            :delete-key-code 8}
                           [controls]
                           [background
                            {:color "#aaa"}]]]])})))

(defn draw-graph []
  [:div
   [:h1 "Graph View"]
   [:div {:style {:width 600 :height 300}}
    [flow-panel]]])
