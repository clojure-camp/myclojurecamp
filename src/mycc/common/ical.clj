(ns mycc.common.ical
  (:require
    [clojure.string :as string]))

(defn format-instant
  ;; iCAL expects datetimes in the form: "20211108T140000Z"
  [instant]
  (string/replace (str instant) #"-|:|(\.\d+)" ""))

(defn escape-text
  ;; RFC 5545 TEXT values; newlines would otherwise start new properties
  [value]
  (string/escape (str value)
                 {\\ "\\\\"
                  \; "\\;"
                  \, "\\,"
                  \newline "\\n"
                  \return ""}))

#_(escape-text "a,b;c\nATTENDEE:x")

(defn ->ics
  [{:keys [uid summary description location start end attendee-emails]}]
  (->> (concat
         [["BEGIN" "VCALENDAR"]
          ["VERSION" "2.0"]
          ["PRODID" "CLOJURE.CAMP"]
          ["CALSCALE" "GREGORIAN"]
          ["METHOD" "PUBLISH"]
          ["BEGIN" "VEVENT"]
          ["SUMMARY" (escape-text summary)]
          ["ORGANIZER" "mailto:bot@clojure.camp"]]
         (for [email attendee-emails]
           ["ATTENDEE" (str "mailto:" email)])
         [["UID" (escape-text uid)]
          ["DESCRIPTION" (escape-text description)]
          ["LOCATION" (escape-text location)]
          ["DTSTART" (format-instant start)]
          ["DTEND" (format-instant end)]
          ["DTSTAMP" (format-instant (.toInstant (java.util.Date.)))]
          ["END" "VEVENT"]
          ["END" "VCALENDAR"]])
       (map (fn [[k v]] (str k ":" v)))
       ;; maximum line length is 75
       (map (fn [line]
              (->> (partition-all 74 line)
                   (map (partial apply str))
                   (string/join "\r\n "))))
       (string/join "\n")))
