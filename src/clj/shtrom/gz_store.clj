(ns shtrom.gz-store
  (:require [clojure.java.io :as io])
  (:import [shtrom.util IOUtil]))

;;; bistファイルをgzip圧縮して扱うモジュール
;;; 以下のルールでgzip圧縮/展開を行う
;;; - bist-read時に、指定されたファイルが存在すれば、そこから読む。
;;;   もし存在しない場合は、末尾に ".gz" をつけたファイルから
;;;   指定されたファイルへとgzip展開を行い、そこから読む。
;;; - bist-write時は、末尾に ".gz" をつけたファイルへと
;;;   gzip圧縮した状態で書き出す。
;;;   またこの際に ".gz" なしの古いファイルが存在する場合、
;;;   そのファイルは削除する。
;;; - bist-read / bist-write の実行時およびサーバプロセスの終了時に、
;;;   一定時間アクセスのない ".gz" なしファイルは削除する。
;;;   (ただし例外として ".gz" つきファイルが存在しないものについては、
;;;   以前のバージョンからのbackward compatibilityの為、削除せずに残す)

;;; TODO: Support to delete older entries by TTL (using another thread)
;;; TODO: bistの更新処理自体もこのモジュールに移動させる(gz絡みの処理がある為)

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

(defn delete-cache-entry! [path & [hook]]
  ;; NB: bistの更新時は明示的にこの関数を呼び、既存のエントリを消す必要がある
  ;; NB: エントリを消すと同時に *.bist ファイルも消す必要がある
  (swap! cache-table
         (fn [old-table]
           (when hook
             (hook))
           ;; NB: 「*.bist は存在するが *.bist.gz が存在しない」ケースにも
           ;;     対応する必要がある(単に *.bist を消さずに残すだけでよい)
           (when (and
                   (.exists (io/file (gz-path path)))
                   (.exists (io/file path)))
             (io/delete-file path true))
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
