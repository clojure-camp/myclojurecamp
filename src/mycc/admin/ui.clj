(ns mycc.admin.ui
  (:require
    [com.rpl.specter :as x]
    [tick.core :as t]
    [mycc.common.date :as date]))

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

(defn bar [c w]
  [:div {:style {:width (str (* w 100) "px")
                 :height "10px"
                 :background-color c}}])

(defn p2p-sessions-per-week
  [users events]
  (when (and (seq users) (seq events))
    (let [user-id->role (zipmap (map :user/id users)
                                (map :user/role users))
          #_#_min-event-t (apply min (map :event/at events))
          min-event-t #inst "2023-12-01"
          start (t/previous-or-same (t/date min-event-t) t/MONDAY)
          end (t/next-or-same (t/date (t/now)) t/MONDAY)
          dates (->> (iterate (fn [d]
                                (t/>> d (t/new-period 1 :weeks)))
                              start)
                     (take-while  (fn [d]
                                    (t/< d end))))
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
          [:td {:colspan 2} "Sessions"]
          [:td {:colspan 2} "Participants (M/S)"]]]
        [:tbody {:tw "text-right font-light tabular-nums"}
         (for [date dates
               :let [session-count (count (grouped-events date))
                     participant-count (count (distinct (mapcat :event/guest-ids (grouped-events date))))
                     per-role-count (frequencies (map user-id->role (distinct (mapcat :event/guest-ids (grouped-events date)))))]]
           [:tr
            [:td (str date)]
            [:td session-count]
            [:td [bar "blue" (/ session-count 20)]]
            [:td participant-count]
            [:td
             [:div {:class "flex"
                    :title (str " (" (per-role-count :role/mentor) "M " (per-role-count :role/student) "S)")}
              [bar "blue" (/ (or (per-role-count :role/mentor) 0) 10)]
              [bar "orange" (/ (or (per-role-count :role/student) 0) 10)]]]])]]])))

#_(p2p-sessions-per-week (mycc.common.db/get-users)
                         (mycc.common.db/get-entities :event))

(defn t-range [start end unit]
  (->> (iterate (fn [d]
                  (t/>> d unit))
                start)
       (take-while (fn [d]
                     (t/< d end)))))

(defn supply-and-demand-view
  [{:keys [label users items user->item-ids item->id item->label]}]
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
       [:td "Student #"]
       [:td "Mentor #"]]]
     [:tbody {:tw "font-light tabular-nums text-right"}
      (for [item (->> items
                      (sort-by (juxt (comp student-item-counts item->id)
                                     (comp mentor-item-counts item->id)))
                      reverse)]
        ^{:key (item->id item)}
        [:tr
         [:td (item->label item)]
         [:td (student-item-counts (item->id item))]
         [:td (mentor-item-counts (item->id item))]])]]))

(defn availability-view [users]
  [:div {}
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
         t-end (date/convert-time [:friday 21] "UTC-12:00" now)
         hours (t-range t-start t-end (t/of-hours 1))]
     [:table
      [:thead
       [:tr
        [:td {:colspan 2} "Availability"]
        [:td {:colspan 2} "S"]
        [:td {:colspan 2} "M"]]]
      [:tbody {:class "font-light tabular-nums text-right"}
       (for [hour hours]
         [:tr
          [:td (str (when (= 0 (t/hour hour))
                      (t/date hour)))]
          [:td [:div {:title (str (t/in hour (t/zone "America/Toronto")))}
                (str (t/hour hour))]]

          [:td {:class "px-2"} (t->count-students hour)]
          [:td [bar "#000" (/ (or (t->count-students hour) 0) 10)]]
          [:td {:class "px-2"} (t->count-mentors hour)]
          [:td [bar "#000" (/ (or (t->count-mentors hour) 0) 10)]]])]])])

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


(defn reports-view
  [{:keys [users topics events]}]
  [:div {:class "space-y-6"}
   #_[users-table-view users]
   [p2p-sessions-per-week users events]

   [availability-view users]

   [supply-and-demand-view
      {:label "Topic"
       :items topics
       :item->id :topic/id
       :item->label :topic/name
       :users users
       :user->item-ids :user/topic-ids}]

   [supply-and-demand-view
      {:label "Language"
       :items (->> users
                   (mapcat (fn [x]
                             (concat (:user/primary-languages x)
                                     (:user/secondary-languages x))))
                   distinct)
       :item->id identity
       :item->label identity
       :users users
       :user->item-ids (fn [u]
                         (concat (:user/primary-languages u)
                                 (:user/secondary-languages u)))}]

   (let [offsets (time-zone-offsets users)]
     [supply-and-demand-view
      {:label "Time Zone"
       :items (->> users
                   (map :user/time-zone)
                   (map offsets)
                   distinct)
       :item->id identity
       :item->label identity
       :users users
       :user->item-ids (fn [u]
                         [(offsets (:user/time-zone u))])}])
   ])


