(ns jsonl-clj.benchmark
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [jsonl-clj.core :as core]
            [clambda.core :as jl]
            [cheshire.core :as cheshire]))

(defonce test-file ;; 120,000 entries (12.24MB) VS 2-core CPU
  "/home/dimitris/Desktop/jsonl-12MB.txt")

(defn- read-json-string
  [s]
  (json/read-str s :key-fn keyword))
;;===================================

(defn- lazy-parse ;; serial
  []
  (time ;; avg = 850ms
    (with-open [rdr (io/reader test-file)]
      (->> rdr
           line-seq
           (map read-json-string)
           (filter :active)
           doall))))

(defn- trance-parse ;; serial
  []
  (time ;; avg = 713ms
    (->> test-file
         io/reader
         jl/lines-reducible
         (core/read-jsonl read-json-string)
         (into [] (filter :active)))))

(defn- pstream-parse ;; parallel
  []
  (time ;; avg = 430ms
    (->> test-file
         io/file
         (core/pread-jsonl read-json-string into)
         (transduce (filter :active) conj))))

;;=========<SAME FILE AS ABOVE BUT VALID JSON>============

(defn- cheshire-parse
  []
  (let [test-file "/home/dimitris/Desktop/json-12MB.txt"]
    (time ;; avg = 377ms (yes, Jackson is fast BUT we lose transducers)
      (with-open [rdr (io/reader test-file)]
        (cheshire/parse-stream rdr keyword)))))

(defn djson-parse
  []
  (let [test-file "/home/dimitris/Desktop/json-12MB.txt"]
    (time ;; avg = 691ms
      (with-open [rdr (io/reader test-file)]
        (json/read rdr :key-fn keyword))))
  )