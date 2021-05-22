(ns app.graph-panel
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            [applied-science.js-interop :as j]
            [app.layout :as layout-lib]
            ["react-flow-renderer" :default ReactFlow :refer [Background Controls ReactFlowProvider]]))
(def react-flow-pro (r/adapt-react-class ReactFlowProvider))
(def react-flow (r/adapt-react-class ReactFlow))
(def background (r/adapt-react-class Background))
(def controls (r/adapt-react-class Controls))

;(defonce graph (r/atom []))

(defn- make-random-id []
  (str (random-uuid)))

(defn- make-edge [source target]
  {:id (str source "->" target) :source source :target target})

(defn- add-edge [!graph source target]
  (swap! !graph conj (make-edge source target)))

(defn- add-node [!graph node]
  (swap! !graph conj node))

(defn update-node-position
  "Update node for node-id to given position inside graph atom."
  [!graph node-id x y]
  (swap! !graph #(mapv (fn [el]
                         (if (= (:id el) node-id)
                           (assoc el :position {:x x :y y})
                           el))

                       %)))

(defn- remove-node [graph node]
  ; TODO
  ; (swap! graph conj node)
  )

(defn- make-node [label position-x position-y]
  (let [!state (r/atom "")
        uid (make-random-id)]
    {:id uid
     :label label
     :style {:fontSize 14
             :fontFamily "monospace"
             :wordBreak "break-word"
             :width 200
             :height 40}
     :data {:label (r/as-element
                    [:div
                     label [:br]
                     [:input
                      {:on-change (fn [e]
                                    (reset! !state (-> e .-target .-value)))}]])}

     :sourcePosition "right"
     :targetPosition "left"
     :position {:x position-x :y position-y}}))

(defn create-elements []
  (let [v1 (make-node "1" 50 200)
        v2 (make-node "2" 300 100)
        v3 (make-node "3" 300 300)
        v4 (make-node "4" 550 200)
        v5 (make-node "6" 50 500)
        v6 (make-node "7" 550 500)]
    (r/atom [v1
             v2
             v3
             v4
             v5
             v6
             (make-edge (:id v1) (:id v2))
             (make-edge (:id v2) (:id v4))
             (make-edge (:id v3) (:id v4))])))

(defn on-element-remove
  "Remove given node from graph."
  [hovered-node-id elements event]
  (let [id (j/get-in event [0 :id])]
    ; Remove from list
    (reset! hovered-node-id nil)
    (swap! elements (fn [elems]
                      (remove (fn [x*]
                                (or
                                 ; Remove the node
                                 (= (:id x*) id)
                                 ; Remove edges to or from
                                 (= (:source x*) id)
                                 (= (:target x*) id)))
                              elems)))))

(defn- handle-keys [!panel-state graph event]
;  (let [
;        target (.-target event)
;        key (.-key event)
;        is-input (instance? js/HTMLInputElement target)
;        hovered-node-id (r/cursor !panel-state [:hovered-node-id])
;        ]
;    ; Add a node if the node if we aren't typing in an inbox or
;    (if (and (= key "a")
;             (not is-input)
;             (nil? @hovered-node-id))
;      (let [
;            positionX 0
;            positionY 0
;            node (make-node "new node" positionX positionY)
;            ]
;        (swap! graph conj node)
;        )))
  )

(defn- on-node-mouse-enter [elements hovered-node-id _ node]
  (reset! hovered-node-id (.-id node)) ; Note that node is an js object, hence .- accessor
  )

(defn- on-node-mouse-leave [elements hovered-node-id _ node]
  (reset! hovered-node-id nil))

(defn get-coordinates
  "Get coordinates within pane for a click event, given reference to container."
  [react-flow-wrapper event]
  (let [bounds (.getBoundingClientRect react-flow-wrapper)
        left (.-left bounds)
        top (.-top bounds)
        client-x (.-clientX event)
        client-y (.-clientY event)]
    {:x (- client-x left)
     :y (- client-y top)}))

(defn- on-pane-click [!panel-state !graph event]
  (let [coords (get-coordinates @(r/cursor !panel-state [:ref]) event)
        node (make-node "new node" (:x coords) (:y coords))]
    (swap! !graph conj node)))

(defn on-node-drag-stop [!graph event node]
  (let [id (.-id node)
        x (.. node -position -x)
        y (.. node -position -y)]
    (update-node-position !graph id x y)))

(defn- flow-panel []
  (let [hovered-node-id (r/atom nil)
        !graph (create-elements)
        !panel-state (r/atom {:hovered-node-id nil
                              :mouse {:x 0 :y 0}
                              :ref nil})
        !hovered-node-id (r/cursor !panel-state [:hovered-node-id])
        handle-keys* (partial handle-keys !panel-state !graph)]

    (r/create-class
     {:component-did-mount (fn []
                             (js/window.addEventListener "keydown" handle-keys*))
      :component-will-unmount (fn []
                                (js/window.removeEventListener "keydown" handle-keys*))
      :reagent-render (fn []
                        [:div
                         [:input {:type "button"
                                  :value "Layout"
                                  :on-click #(do
                                               (swap! !graph layout-lib/layout-graph))}]
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
                            :elements @!graph
                            :on-node-mouse-enter (partial on-node-mouse-enter !graph !hovered-node-id)
                            :on-node-mouse-leave (partial on-node-mouse-leave !graph !hovered-node-id)
                            :on-node-drag-stop (fn [event node] (on-node-drag-stop !graph event node))
                            :on-pane-click (partial on-pane-click !panel-state !graph)
                            :on-connect #(let [source (.-source %)
                                               target (.-target %)]
                                           (swap! !graph conj {:id (str/join "->" [source target]) :source source :target target}))
                            :on-elements-remove (fn [event] (on-element-remove !hovered-node-id !graph event))
                            :delete-key-code 8
                            :ref #(reset! (r/cursor !panel-state [:ref]) %)}
                           [controls]
                           [background
                            {:color "#aaa"}]]]])})))

(defn draw-graph []
  [:div
   [:h1 "Graph View"]
   "`Backspace` to delete, click to add node."
   [:div {:style {:width 600 :height 300}}
    [flow-panel]]])
