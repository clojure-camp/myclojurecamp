(ns mycc.common.ui
  (:require
    [bloom.commons.fontawesome :as fa]
    [bloom.commons.ui.textarea :as textarea]
    [mycc.common.colors :as colors]))

(defn popover-view
  [content]
  [:div.info {:tw "relative group"}
   [fa/fa-question-circle-solid
    {:tw "w-4 h-4 text-#ccc"}]
   [:div.popover {:tw "hidden group-hover:block border absolute bg-white p-2 left-1em top-0 min-w-18em font-light"}
    content]])

(defn row
  [{:keys [title subtitle info featured?]} & content]
  [:div.row
   {:tw ["w-full max-w-100vw py-4 px-4 space-y-3 overflow-y-auto overflow-x-auto relative"
         (if featured?
           "border border-4 border-blue-200 p-4"
           "border-b-1")]}
   (when (or title subtitle)
     [:div
      (when title
        [:h1 {:tw "font-bold flex items-center gap-1"}
         title
         (when info
           [popover-view info])])
      (when subtitle
        [:div {:tw "font-light mt-1"}
         subtitle])])
   (into [:div.content {:tw "font-light space-y-3"}]
         content)])

(defn checkbox-list
  [{:keys [value choices on-change direction]}]
  {:pre [(set? value)]}
  [:div.checkbox-list {:tw ["flex gap-y-3 flex-wrap gap-x-4"
                            (when (= direction :vertical)
                              "flex-col")]}
   (for [[choice-value choice-label] choices]
     ^{:key (or choice-value "nil")}
     [:label {:tw "cursor-pointer flex gap-1"}
      [:input {:type "checkbox"
               :checked (contains? value choice-value)
               :on-change (fn [_]
                            (on-change ((if (contains? value choice-value)
                                          disj
                                          conj)
                                        value
                                        choice-value)
                                       (if (contains? value choice-value)
                                         :remove
                                         :add)
                                       choice-value))}]
      [:span.label choice-label]])])

(defn radio-list
  [{:keys [value choices on-change direction]}]
  [:div.radio-list {:tw ["flex gap-y-3 gap-x-4"
                         (when (= direction :vertical)
                           "flex-col")]}
   (for [[choice-value choice-label] choices]
     ^{:key (or choice-value "nil")}
     [:label {:tw ["cursor-pointer flex gap-1"
                   (if (= direction :vertical)
                     "items-baseline"
                     "items-center")]}
      [:input {:type "radio"
               :checked (= value choice-value)
               :on-change (fn [_]
                            (on-change choice-value))}]
      [:span.label choice-label]])])

(defn input
  [opts]
  [:input (assoc opts
            :tw ["p-1 border border-gray-300 font-light"
                 (when (:disabled opts)
                   "bg-gray-200")])])

(defn textarea
  [opts]
  [textarea/textarea
   (assoc opts
     :tw "p-1 border border-gray-300 font-light w-full h-5em")])

(defn button
  [opts content]
  [:button (assoc opts
             :tw "text-white font-light px-2 py-1 rounded cursor-pointer bg-clojure-blue hover:bg-clojure-blue-darker border border-clojure-blue-darker")
   content])

(defn secondary-button
  [opts content]
  [:button (assoc opts
             :tw "text-black font-light px-2 py-1 rounded cursor-pointer bg-gray-200 hover:bg-gray-300 text-xs border border-gray-400")
   content])

(defn warning
  [opts content]
  [:div.warning {:tw "bg-#ffe9e9 text-red-500 flex p-1 mb-4 rounded border border-#ffc4c4 gap-1"}
   [fa/fa-exclamation-triangle-solid {:tw "w-1em h-1em"}]
   content])
