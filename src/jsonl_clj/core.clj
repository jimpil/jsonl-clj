(ns jsonl-clj.core
  (:require [clambda.core :as jl]
            [clojure.java.io :as io])
  (:import [java.io Writer File]
           [java.nio.file Files]))

(defn read-jsonl
  "Returns an `eduction` encapsulating the computation for parsing <reducible-in>.
   <read-json-str> should be a fn of 1-arg (`data.json/read-str` or `cheshire/parse-string`
   are good candidates)."
  [read-json-str reducible-in]
  (eduction (map read-json-str) reducible-in))

(defn pread-jsonl
  "Parallel version of `read-jsonl`.
   Relies on the parallel Stream returned by `Files/lines`,
   and therefore it requires at least Java-9 for the
   expected/intuitive performance improvements.

   <in> should be a `java.io.File` object.

   <read-json-str> is the fn that will do the actual parsing
   (`data.json/read-str` or `cheshire/parse-string` are good candidates).

   <combine-f> is the fn that will combine the results from the
   various threads, so it depends on the transducing context in which
   the `eduction` returned will be eventually used. For example,
   if the end-goal is collecting everything, then use `into` as
   the <combine-f> here, and `conj` as the reducing-f.

   See `clambda.core/stream-into` for an example of a collecting context,
   and `clambda.core/stream-some` for an example of a short-circuiting one."
  [read-json-str combine-f ^File in]
  (read-jsonl
    read-json-str
    (-> in
        .toPath
        Files/lines
        .parallel
        (jl/stream-reducible combine-f))))

(defn- trun!
  "Like `clojure.core/run!`, but based on `transduce`."
  [xform proc coll]
  (transduce
    xform
    (fn
      ([x] x)
      ([_ x] (proc x)))
    nil
    coll))


(defn write-jsonl
  "Writes <xs> (anything compatible with `reduce`) as JSONLines using
   <write-json-str> as the fn that does the actual JSON writing
   (`data.json/write` or `(fn [o _] (cheshire/generate-string o))` are good candidates).

    Must be used inside a `with-open` expression passing the writer as the 2nd argument."
  ([write-json-str ^Writer wr xs]
   (write-jsonl write-json-str wr (map identity) xs))
  ([write-json-str ^Writer wr xform xs]
   (let [ls (System/getProperty "line.separator")]
     (trun!
       xform
       (fn [x]
         (write-json-str x wr)
         (.write wr ls))
       xs)
     (.flush wr))))


(defn jsonl->json
  ([in-file out-file]
   (jsonl->json "records" in-file out-file))
  ([tk in-file out-file]
   (jsonl->json tk true in-file out-file))
  ([tk newlines? in-file out-file]
   (let [ls (when newlines?
              (System/getProperty "line.separator"))]
     (with-open [wrt (io/writer out-file)]
       ;; open top-level object with a single key <tk> mapped to array
       (.write wrt (str "{" (pr-str tk) ": [" ls))
       ;; write all the lines
       ;; interposed by comma (and optionally newline)
       (->> in-file
            io/reader
            jl/lines-reducible
            (trun!
              (interpose (str "," ls))
              #(.write wrt ^String %)))
       ;; close the array & the top-level object
       (.write wrt " ]}")
       ;; write final newline
       (.write wrt ls)
       (.flush wrt)))))

(comment
  ;; copy (by means of streaming) from one file to another
  ;; transforming with `transform-fn` along the way - no laziness!
  (with-open [wrt (io/writer "destination-file")]
    (->> "input-file" ;; it doesn't matter how big this is
         io/reader
         jl/lines-reducible
         (read-jsonl data.json/read-str)
         (write-jsonl data.json/write wrt (map transform-fn))))
  )
