(ns app.layout
  (:require
   ["dagre" :as dagre]))

(defn- make-empty-dagre-graph []
  (let [Graph (.. dagre -graphlib -Graph)
        dagre-graph (Graph.)]
    (.setDefaultEdgeLabel dagre-graph #(clj->js {}))
    (.setGraph dagre-graph (clj->js {:rankdir "LR"}))
    dagre-graph))

(defn- update-position-from-dagre-node [dagre-graph el]
  (let [node-with-pos (.node dagre-graph (:id el))]
    (if (:data el) ; This is node, not an edge
      (do
        (assoc el :position {:x (.-x node-with-pos)
                             :y (.-y node-with-pos)}))

      el)))

(defn layout-graph
  "Layout graph using dagre"
  [elements]
  (let [dagre-graph (make-empty-dagre-graph)
        _ (doseq [el elements]
      ; TODO: get width & height from node
            (if (contains? el :source)
              (.setEdge dagre-graph (:source el) (:target el))
              (.setNode dagre-graph (:id el) (clj->js {:width 200 :height 40}))))
        _ (.layout dagre dagre-graph)
        elements* (mapv (partial update-position-from-dagre-node dagre-graph) elements)]
    elements*))
