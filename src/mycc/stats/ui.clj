(ns mycc.stats.ui
  (:require
    [com.rpl.specter :as x]
    [tick.core :as t]
    [mycc.common.date :as date]))

(def color
  {:role/student "orange"
   :role/mentor "blue"})

(defn offset [zone]
  (t/hours (t/between
             (-> (t/date)
                 (t/at (t/time "11:00"))
                 (t/in (t/zone zone)))
             (-> (t/date)
                 (t/at (t/time "11:00"))
                 (t/in (t/zone "UTC"))))))

(defn users-table-view
  [users]
  [:div
   (let [columns [["Name" :user/name]
                  ["Role" :user/role]
                  ["S?" :user/subscribed?]
                  ["TZ" :user/time-zone]]
         rows (->> users
                   (sort-by (juxt :user/role :user/name)))]
     [:table
      [:thead
       [:tr
        (for [[l _] columns]
          [:td l])]]
      [:tbody
       (for [row rows]
         [:tr
          (for [[_ f] columns]
            [:td {} (str (f row))])])]])])

(defn bar
  [color item-count max-expected-count]
  [:div {:style {:width (str (* (/ (or item-count 0)
                                   max-expected-count) 100) "px")
                 :height "10px"
                 :background-color color}}])

(defn p2p-sessions-per-week
  [users events]
  (when (and (seq users) (seq events))
    (let [user-id->role (zipmap (map :user/id users)
                                (map :user/role users))
          #_#_min-event-t (apply min (map :event/at events))
          min-event-t #inst "2023-12-01"
          start (t/previous-or-same (t/date min-event-t) t/MONDAY)
          end (t/next (t/next-or-same (t/date (t/now)) t/MONDAY) t/MONDAY)
          dates (->> (iterate (fn [d]
                                (t/>> d (t/new-period 1 :weeks)))
                              start)
                     (take-while  (fn [d]
                                    (t/< d end))))
          grouped-opt-in-counts (->> users
                                     #_(mycc.common.db/get-users)
                                     (group-by :user/role)
                                     (x/transform [x/MAP-VALS] (fn [users]
                                                                 (->> users
                                                                      (mapcat :user/pair-opt-in-history)
                                                                      frequencies))))
          grouped-events (->> events
                              (group-by
                                (fn [e]
                                  (t/previous-or-same
                                    (t/date (:event/at e))
                                    t/MONDAY))))]
      [:div
       [:h1 "P2P Participants"]
       [:table
        [:thead
         [:tr
          [:td "Week of"]
          [:td {:colspan 2} "Opt-Ins (S/M)"]
          [:td {:colspan 2} "Participants (S/M)"]
          [:td {:colspan 2} "Sessions"]]]
        [:tbody
         {:style {:font-weight "lighter"
                  :font-variant-numeric "tabular-nums"
                  :text-align "right"}}
         (for [date dates
               :let [session-count (count (grouped-events date))
                     participant-count (count (distinct (mapcat :event/guest-ids (grouped-events date))))
                     per-role-count (frequencies (map user-id->role (distinct (mapcat :event/guest-ids (grouped-events date)))))]]
           [:tr
            [:td (str date)]
            ;; opt-ins
            [:td (+ (get-in grouped-opt-in-counts [:role/student date] 0)
                    (get-in grouped-opt-in-counts [:role/mentor date] 0))]
            [:td
             [:div {:class "flex"
                    :title (str " (" (get-in grouped-opt-in-counts [:role/student date]) "S"
                                " " (get-in grouped-opt-in-counts [:role/mentor date]) "M"  ")")}
              [bar (:role/student color) (get-in grouped-opt-in-counts [:role/student date]) 10]
              [bar (:role/mentor color) (get-in grouped-opt-in-counts [:role/mentor date]) 10]]]
            ;; participants
            [:td participant-count]
            [:td
             [:div {:class "flex"
                    :title (str " (" (per-role-count :role/student) "S"
                                " " (per-role-count :role/mentor) "M"  ")")}
              [bar (:role/student color) (per-role-count :role/student) 10]
              [bar (:role/mentor color) (per-role-count :role/mentor) 10]]]
            ;; sessions
            [:td session-count]
            [:td [bar "purple" session-count 20]]])]]])))

#_(p2p-sessions-per-week (mycc.common.db/get-users)
                         (mycc.common.db/get-entities :event))

(defn t-range [start end unit]
  (->> (iterate (fn [d]
                  (t/>> d unit))
                start)
       (take-while (fn [d]
                     (t/< d end)))))

(defn supply-and-demand-view
  [{:keys [label users items user->item-ids item->id item->label sort]}]
  (let [users-by-role (->> users
                           (group-by :user/role))
        student-item-counts (->> users-by-role
                                 :role/student
                                 (mapcat user->item-ids)
                                 frequencies)
        mentor-item-counts (->> users-by-role
                                :role/mentor
                                (mapcat user->item-ids)
                                frequencies)]
    [:table
     [:thead
      [:tr
       [:td label]
       [:td {:colspan 2} "Student #"]
       [:td {:colspan 2} "Mentor #"]]]
     [:tbody
      {:style {:font-weight "lighter"
               :font-variant-numeric "tabular-nums"
               :text-align "right"}}
      (let [sort-fn (case sort
                      :rank
                      #(sort-by (juxt (comp student-item-counts item->id)
                                      (comp mentor-item-counts item->id))
                                %)
                      :label
                      #(sort-by item->label %))]
        (for [item (->> items
                        sort-fn
                        reverse)]
          ^{:key (item->id item)}
          [:tr
           [:td (item->label item)]
           [:td (student-item-counts (item->id item))]
           [:td [bar (:role/student color) (student-item-counts (item->id item)) 10]]
           [:td (mentor-item-counts (item->id item))]
           [:td [bar (:role/mentor color) (mentor-item-counts (item->id item)) 10]]]))]]))

(defn availability-view [users]
  (let [now (t/date)
        ->t->count (fn [users]
                     (->> users
                          (filter :user/time-zone)
                          (filter :user/availability)
                          (mapcat (fn [u]
                                    (->> u
                                         (x/transform [:user/availability x/MAP-KEYS]
                                           (fn [x]
                                             (date/convert-time x (:user/time-zone u) now)))
                                         (x/transform [:user/availability]
                                           (fn [x]
                                             (filter second x)))
                                         :user/availability
                                         keys)))
                          frequencies))
        grouped-users (group-by :user/role users)
        t->count-students (->t->count (:role/student grouped-users))
        t->count-mentors (->t->count (:role/mentor grouped-users))
        t-start (date/convert-time [:monday 9] "UTC+14:00" now)
        t-end (date/convert-time [:sunday 21] "UTC-12:00" (t/date))
        hours (t-range t-start t-end (t/of-hours 1))
        days (partition-all 24 hours) #_[hours]]
    [:section
     [:h1 "Availability - Supply and Demand"]
     [:p "Times are in UTC"]
     [:div {:style {:display "flex"
                    :max-width "90vw"
                    :overflow-x "auto"
                    :gap "2em"
                    :align-items "flex-start"}}
      (for [day days]
        [:table
         [:thead
          [:tr
           [:td {:colspan 2} [:span {:style {:white-space "nowrap"}} "Day & Hour"]]
           [:td {:colspan 2} "S"]
           [:td {:colspan 2} "M"]]]
         [:tbody
          {:style {:font-weight "lighter"
                   :font-variant-numeric "tabular-nums"
                   :text-align "right"}}
          (for [hour day]
            [:tr
             [:td (str (when (= 0 (t/hour hour))
                         (t/day-of-week (t/date hour))))]
             [:td [:div {:title (str (t/in hour (t/zone "America/Toronto")))}
                   (str (t/hour hour))]]

             [:td {:class "px-2"} (t->count-students hour)]
             [:td [bar (:role/student color) (t->count-students hour) 20]]
             [:td {:class "px-2"} (t->count-mentors hour)]
             [:td [bar (:role/mentor color) (t->count-mentors hour) 20]]])]])]]))

#_(availability-view (mycc.common.db/get-users))

(defn time-zone-offsets [users]
  (->> users
       (keep :user/time-zone)
       distinct
       (map (juxt identity offset))
       (into {})))

#_(let [timezones-of-interest ["America/Vancouver"
                               "America/Toronto"
                               "Europe/London"
                               "Europe/Helsinki"
                               "Asia/Calcutta"
                               "Asia/Manila"]
        reference-time-zone "America/Toronto"
        reference-hour 19])

(defn p2p-events-view [users events]
  (let [cutoff (t/previous-or-same (t/date) t/MONDAY)
        id->user (zipmap (map :user/id users)
                         users)]
    [:section
     [:h1 "P2P Sessions This Week"]
     [:p "Times in UTC"]
     [:table
      [:tbody
       (for [event (->> events
                        (filter (fn [e]
                                  (t/<= cutoff (t/date (:event/at e)))))
                        (sort-by :event/at))]
         [:tr
          [:td (date/format (:event/at event) "yyyy-MM-dd HH:mm")]
          (for [user (->> (:event/guest-ids event)
                          (map id->user)
                          (sort-by :user/role))]
            [:td
             [:span {:style {:color (color (:user/role user))}}
              (:user/name user)]])])]]]))

(defn reports-view
  [{:keys [users topics events]}]
  [:div {:style {:display "flex"
                 :flex-direction "column"
                 :align-items "flex-start"
                 :gap "4em"}}
   #_[users-table-view users]

   [p2p-events-view users events]

   [p2p-sessions-per-week users events]

   [availability-view users]

   [supply-and-demand-view
    {:label "Topic"
     :items topics
     :sort :rank
     :item->id :topic/id
     :item->label :topic/name
     :users users
     :user->item-ids :user/topic-ids}]

   [supply-and-demand-view
    {:label "Primary Language"
     :items (->> users
                 (mapcat (fn [x]
                           (concat (:user/primary-languages x)
                                   #_(:user/secondary-languages x))))
                 distinct)
     :sort :rank
     :item->id identity
     :item->label identity
     :users users
     :user->item-ids (fn [u]
                       (concat (:user/primary-languages u)
                               #_(:user/secondary-languages u)))}]

   [supply-and-demand-view
    {:label "Secondary Language"
     :items (->> users
                 (mapcat (fn [x]
                           (concat #_(:user/primary-languages x)
                                   (:user/secondary-languages x))))
                 distinct)
     :sort :rank
     :item->id identity
     :item->label identity
     :users users
     :user->item-ids (fn [u]
                       (concat #_(:user/primary-languages u)
                               (:user/secondary-languages u)))}]

   (let [offsets (time-zone-offsets users)]
     [supply-and-demand-view
      {:label "Time Zone"
       :items (->> users
                   (map :user/time-zone)
                   (map offsets)
                   distinct)
       :sort :label
       :item->id identity
       :item->label identity
       :users users
       :user->item-ids (fn [u]
                         [(offsets (:user/time-zone u))])}])
   ])


