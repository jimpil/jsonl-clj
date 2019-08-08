# jsonl-clj

DEPRECATED - SEE [clambda](https://github.com/jimpil/clambda) and its `clambda.line-streams` namespace.

A (embarrassingly) small library designed for streaming [JSON Lines](http://jsonlines.org/).

## Why 
`JSON Lines` is gaining popularity, in no small part, due to its delimited nature which makes parsing/emitting/streaming it trivial. The basic premise is that instead of parsing a giant nested `JSON` object containing N entries, we can parse those N entries directly (as `JSON Lines`), and potentially in parallel, as the input is now splittable (on newlines). Even the fastest `JSON` parsers out there (e.g. Jackson) have an upper limit simply because they are typically serial. In short, the possibility for parallelism in a straight-forward and composable way is a win.


Clojure is no stranger to streaming. In fact, it provides great tools for streaming-like semantics, most notably laziness and transducers. The lazy approach is out-of-scope for this project as it can be implemented by anyone in 3 lines (and as we shall see is not very useful for big files). The transducer-based version includes a serial version and a parallel one (backed by Java Streams). 

 
## Where
FIXME 
  
## Usage
First things first:
 
```clj
(require '[json-clj.core :as core] 
         '[clambda.core :as jl]
         '[clojure.java.io :as io]
         '[clojure.data.json :as json])
```

#### read-jsonl \[f in\]

Uses `f` to parse JSON objects from a reducible `in` (typically the result of `(lines-reducible (io/reader ...))`). Returns an `eduction` so no actual work is done. Can be further transformed/filtered etc before a transducing context pulls the trigger.

```clj
(->> "INPUT.TXT"
     io/reader
     jl/lines-reducible
     (core/read-jsonl #(json/read-str % :key-fn keyword))
     (into [] (filter :active)))
```

#### pread-jsonl \[f cf ^File in\]
Very similar to `read-jsonl`. `cf` is expected to the fn that will combine the results from the various threads, so it depends on the transducing context in which the `eduction` returned, will eventually be used (e.g. in a collecting context the appropriate combiner would be `into`). **Only useful on Java9 and above!**

```clj
(->> "INPUT.TXT"
     io/file
     (core/pread-jsonl #(json/read-str % :key-fn keyword) into)
     (transduce (filter :active) conj []))
``` 

#### write-jsonl \[f ^Writer wrt xform xs\]

Writes the `xs` (anything compatible with `transduce`) with writer `wrt`, via `f`, in `JSON Lines` format (each object on its own line).

```clj
;; copy (by means of streaming) from one file to another
;; transforming with `transform-fn` along the way - no laziness!
(with-open [wrt (io/writer "destination-file.jsonl")]
  (->> "input-file.jsonl" ;; it doesn't matter how big this is
       io/reader
       jl/lines-reducible
       (core/read-jsonl json/read-str)
       (core/write-jsonl json/write wrt (map transform-fn))))
```

## Performance

See the `jsonl-clj.benchmark` namespace for code that you can run yourself. My takeaway is that on this humble dual-core laptop I can't quite get Jackson level performance, even with the parallel version, but I'm pleasantly close! I bet on a quad-core (or higher) things would be different, and that's not even the main point here. The main benefit of this project is the fact that further compositions can be made by the caller at practically no cost (via the beauty of transducers and/or Streams of course). That is not typically something you can do with JSON parsers (to the best of my knowledge), but the `JSON Lines` convention was conceived precisely for this reason.

## TL;DR
Yes, Jackson is FAST! But can it compete with a parallel parser working against the `JSON Lines` convention, on a multi-core CPU? I got rather close with a modest dual-core CPU. 

## License

Copyright © 2019 Dimitrios Piliouras

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
