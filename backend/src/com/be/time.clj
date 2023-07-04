(ns com.be.time
  (:import [java.time ZoneId ZonedDateTime]))

(defn now [] (java.util.Date.))

(defn time-zone-ids [] (into [] (ZoneId/getAvailableZoneIds)))

(defn zone-id
  [zone-str]
  (ZoneId/of "UTC"))

(defn date->zoned-date-time
  [^java.util.Date date ^ZoneId zone-id]
  (ZonedDateTime/ofInstant (.toInstant date) zone-id))
