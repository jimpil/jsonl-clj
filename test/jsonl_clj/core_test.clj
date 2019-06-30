(ns jsonl-clj.core-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [jsonl-clj.core :refer :all]
            [clojure.java.io :as io]
            [clambda.core :as jl])
  (:import (java.io StringReader StringWriter)))

(def sample
  "{\"id\":1,\"father\":\"Mark\",\"mother\":\"Charlotte\",\"children\":[\"Tom\"]}\n{\"id\":2,\"father\":\"John\",\"mother\":\"Ann\",\"children\":[\"Jessika\",\"Antony\",\"Jack\"]}\n{\"id\":3,\"father\":\"Bob\",\"mother\":\"Monika\",\"children\":[\"Jerry\",\"Karol\"]}\n"
  )

(defonce expected
  [{:id 1
    :father "Mark"
    :mother "Charlotte"
    :children ["Tom"]}
   {:id 2
    :father "John"
    :mother "Ann"
    :children ["Jessika" "Antony" "Jack"]}
   {:id 3
    :father "Bob"
    :mother "Monika"
    :children ["Jerry" "Karol"]}])


(deftest read-jsonl-tests
  (testing "parsing JSONLines"
    (let [lines (jl/lines-reducible (io/reader (StringReader. sample)))
          parsed (read-jsonl #(json/read-str % :key-fn keyword) lines)]
      (is (= expected (into [] parsed)))))
  )

(deftest write-jsonl-tests
  (testing "emitting JSONLines"
    (with-open [wrt (StringWriter.)]

      (write-jsonl #(json/write %1 %2 :key-fn name) wrt expected)

      (is (= sample (.toString wrt))))
    )

  )
