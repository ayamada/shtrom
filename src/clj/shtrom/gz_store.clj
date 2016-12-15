(ns shtrom.gz-store
  (:require [clojure.java.io :as io])
  (:import [shtrom.util IOUtil]))

;;; TODO: Support to delete older entries by TTL (using another thread)
;;; TODO: プロセス終了時に delete-all-cache-entries! を呼ぶようにする

(defn- gz-path [path]
  (str path ".gz"))

(defn gunzip!
  "Gunzip from \"*.bist.gz\" file to \"*.bist\" file"
  [bist-path]
  ;; NB: 「*.bist は存在するが *.bist.gz が存在しない」ケースにも
  ;;     対応する必要がある(gzファイルがない時は何もしなければok)
  (when (.exists (io/file (gz-path bist-path)))
    (IOUtil/bistGunzip (gz-path bist-path) bist-path)))

(defonce cache-table (atom {}))

(defn delete-cache-entry! [path]
  ;; NB: エントリを消すと同時に *.bist ファイルも消す必要がある
  (swap! cache-table
         (fn [old-table]
           ;; NB: 「*.bist は存在するが *.bist.gz が存在しない」ケースにも
           ;;     対応する必要がある(単に *.bist を消さずに残すだけでよい)
           (when (.exists (io/file (gz-path path)))
             (when (.exists (io/file path))
               (io/delete-file path true)))
           (dissoc old-table path))))

(defn delete-all-cache-entries! []
  (doseq [k (keys @cache-table)]
    (delete-cache-entry! k)))

(defn gunzip-bist! [path]
  (swap! cache-table
         (fn [old-table]
           (if-let [entry (get @cache-table path)]
             (do
               (when-not (.exists (io/file path))
                 (gunzip! path))
               old-table)
             (let [entry {:timestamp (System/currentTimeMillis)}]
               (gunzip! path)
               (assoc old-table path entry))))))
