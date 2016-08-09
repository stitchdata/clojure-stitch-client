(ns com.stitchdata.client.example
  (:require [com.stitchdata.client.core :as sc]))

(defn -main [& args]
  (let [[client-id token -namespace] args]
    (with-open [client (sc/client {::sc/client-id (Integer/parseInt client-id)
                                   ::sc/token token
                                   ::sc/namespace -namespace
                                   ::sc/table-name "people"
                                   ::sc/key-names ["id"]})]
      (doseq [[id first-name] [[1 "John"]
                               [2 "Paul"]
                               [3 "George"]
                               [4 "Ringo"]]]
        (sc/push client {::sc/action ::sc/upsert
                         ::sc/sequence (System/currentTimeMillis)
                         ::sc/data {"id" id
                                    "first_name" first-name}}))))
  (println "Sent example records to stitch"))
