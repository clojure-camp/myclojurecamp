(ns mycc.common.colors)

(def accent-light "#4576bf")
(def accent-dark "#2b468d")

(def gray "#f3f3f3")
(def clojure-green "#5FAD31")
(def clojure-blue "#567ED2")
(def clojure-blue-darker "#396CD5")
(def clojure-camp-blue "#181742")
(def light-text "#bbb")

(def palette
  {:accent-light "#4576bf"
   :accent-dark "#2b468d"

   :gray "#f3f3f3"
   :clojure-green "#5FAD31"
   :clojure-blue "#567ED2"
   :clojure-blue-darker "#396CD5"
   :clojure-camp-blue "#181742"
   :light-text "#bbb"})

(defn tailwind-color-styles []
  (for [[label value] palette
        [prefix attr modifier]
        [["bg-" :background-color ""]
         ["text-" :color ""]
         ["hover\\:bg-" :background-color ":hover"]
         ["hover\\:text-" :color ":hover"]]]
    (str "." prefix (name label) modifier " {"
         (name attr) ": " value ";"
         "}")))


