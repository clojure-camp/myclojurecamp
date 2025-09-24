(ns mycc.common.meetupdotcom
  (:require
   [cheshire.core :as json]
   [org.httpkit.client :as http]))

(def query
  "query GetUpcomingEvents($urlname: String!) {
  groupByUrlname(urlname: $urlname) {
    name
    events {
      edges {
        node {
          id
          title
          eventUrl
          dateTime
          series {
            monthlyRecurrence {
              monthlyDayOfWeek
              monthlyWeekOfMonth
            }
          }
        }
      }
    }
  }
}")

(defn meetup-events []
  (-> @(http/request
        {:method :post
         :url "https://api.meetup.com/gql-ext"
         :headers {"Content-Type" "application/json"
                   "User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:138.0) Gecko/20100101 Firefox/138.0"}
         :body (json/generate-string {:query query
                                      :variables {:urlname "clojure-camp"}})})
      :body
      (json/parse-string keyword)))

