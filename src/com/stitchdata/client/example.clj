(ns com.stitchdata.client.example
  (:require [com.stitchdata.client.core :as stitch]))

(defn -main [& args]
  (let [[client-id token -namespace] args]

    (with-open [client (stitch/client {::stitch/client-id (Integer/parseInt client-id)
                                       ::stitch/token token
                                       ::stitch/namespace -namespace
                                       ::stitch/table-name "people"
                                       ::stitch/key-names ["id"]})]
      (doseq [[id first-name] [[1 "John"]
                               [2 "Paul"]
                               [3 "George"]
                               [4 "Ringo"]]]
        (stitch/push client {::stitch/action ::stitch/upsert
                             ::stitch/sequence (System/currentTimeMillis)
                             ::stitch/data {"id" id
                                            "first_name" first-name}})))))
