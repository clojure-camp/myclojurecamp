(ns mycc.common.ui
  (:require
    [bloom.commons.fontawesome :as fa]
    [mycc.common.colors :as colors]))

(defn popover-view
  [content]
  [:div.info {:tw "relative group"}
   [fa/fa-question-circle-solid
    {:tw "w-4 h-4 text-#ccc"}]
   [:div.popover {:tw "hidden group-hover:block border absolute bg-white p-2 w-100vw left-1em top-0 m-w-30em font-light"}
    content]])

(defn row
  [{:keys [title info]} content]
  [:div.row
   {:tw "w-full min-w-30em py-4 border-b-1"}
   (when title
     [:h1 {:tw "font-bold mb-3 flex items-center gap-1"}
      title
      (when info
        [popover-view info])])
   [:div.content {:tw "font-light"}
    content]])

(defn radio-list
  [{:keys [value choices on-change direction]}]
  [:div.radio-list {:tw ["flex gap-3"
                         (when (= direction :vertical)
                           "flex-col")]}
   (for [[choice-value choice-label] choices]
     ^{:key choice-value}
     [:label {:tw "cursor-pointer flex gap-1"}
      [:input {:type "radio"
               :checked (= value choice-value)
               :on-change (fn [_]
                            (on-change choice-value))}]
      [:span.label choice-label]])])

(defn input
  [opts]
  [:input (assoc opts
            :tw "p-1 border border-gray-300 font-light")])

(defn button [opts content]
  [:button (assoc opts
             :tw "text-white font-light px-2 py-1 rounded cursor-pointer bg-clojure-blue hover:bg-clojure-blue-darker")
   content])

(defn warning
  [opts content]
  [:div.warning {:tw "bg-#ffe9e9 text-red-500 flex p-1 mb-4 rounded border border-#ffc4c4 gap-1"}
   [fa/fa-exclamation-triangle-solid {:tw "w-1em h-1em"}]
   content])
