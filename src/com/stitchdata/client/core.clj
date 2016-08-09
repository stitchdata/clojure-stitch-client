(ns com.stitchdata.client.core
  "Clojure Stitch Client

  Usage:

  (ns foo.bar
    (:require [com.stitchdata.client.core :as sc]))

  ;; Build a client in a with-open. The client accumulates records in
  ;; batches, and we should close it when we're done to avoid leaving
  ;; messages in an unsent batch.
  (with-open [stitch (sc/client {::sc/client-id 1234
                                 ::sc/token \"adfadfadfadfadfaadfadfadfagdfa\"
                                 ::sc/namespace \"event_tracking\"})]
    (doseq [data some-source-of-data]
      ;; Send a record to Stitch
      (sc/push stitch {::sc/action ::sc/upsert
                       ::sc/table-name \"events\"
                       ::sc/key-names [\"hostname\" \"timestamp\"]
                       ::sc/sequence (System/currentTimeMillis)
                       ::sc/data data})))
"
  (:import [com.stitchdata.client StitchClientBuilder
            StitchClient StitchMessage StitchMessage$Action]
           [java.util List]))

(set! *warn-on-reflection* true)

;; Helper fns for validating args. When spec comes out we can consider
;; using it for validation. I'd rather not use Scheme because I don't
;; want to introduce unnecessary dependencies.

(defn- action [action-kw]
  (case action-kw
    ::upsert      StitchMessage$Action/UPSERT
    ::switch-view StitchMessage$Action/SWITCH_VIEW
    :else
    (throw (IllegalArgumentException.
            (str "action must be either " ::upsert " or " ::switch-view)))))

(defn- client-id [x]
  (when-not (integer? x)
    (throw (IllegalArgumentException. "client-id must be an integer")))
  x)

(defn- -namespace [x]
  (when-not (string? x)
    (throw (IllegalArgumentException. "namespace must be a string")))
  x)

(defn- token [x]
  (when-not (string? x)
    (throw (IllegalArgumentException. "token must be a string")))
  x)

(defn- table-name [x]
  (when-not (string? x)
    (throw (IllegalArgumentException. "table-name must be a string")))
  x)

(defn- table-version [x]
  (when-not (integer? x)
    (throw (IllegalArgumentException. "table-version must be an integer")))
  x)

(defn- key-names [x]
  (when-not (coll? x)
    (throw (IllegalArgumentException. "key-names must be a collection")))
  x)

(defn- batch-size-bytes [x]
  (when-not (integer? x)
    (throw (IllegalArgumentException. "batch-size-bytes must be an integer")))
  x)

(defn- batch-delay-millis [x]
  (when-not (integer? x)
    (throw (IllegalArgumentException. "batch-delay-millis must be an integer")))
  x)

(defn- -sequence [x]
  (when-not (integer? x)
    (throw (IllegalArgumentException. "sequence must be an integer")))
  x)

(defn client
  "Build a Stitch client.

  Takes a map with the following structure and returns an instance of
  StitchClient:

  {
   ;; [Required] Credentials for Stitch
   ::sc/client-id 1234
   ::sc/token     \"adfadfadfadfadfadfadfadfadfadfadfadfadfadfadfadf\"
   ::sc/namespace \"event_tracking\"

   ;; [Optional] Default message values
   ::sc/table-name    \"events\"
   ::sc/key-names     [\"hostname\" \"timestamp\"]

   ;; [Optional] Batch tuning
   ::sc/batch-size-bytes   1000000 ;; Trigger batch at 1 Mb
   ::sc/batch-delay-millis   10000 ;; Trigger batch at 10 seconds
  }

  Credentials
  -----------

  You should have received these when you set up the integration at
  http://stitchdata.com.

  Default Message Values
  ----------------------

  If all the records you send with this client will be going to the
  same table, you can set a default table name and list of key names
  when you create the client with the ::sc/table-name
  and ::sc/key-names keys. You can then omit those values from the
  messages.

  Batch Tuning
  ------------

  The client will accumulate messages in batches, and send the batch
  when one of the following conditions is met:

    * Accumulated 4 Mb of data
    * Accumulated 10,000 records
    * 1 minute has passed since we sent the last batch

  You can tune the amount of data to require before triggering a batch
  with ::sc/batch-size-bytes. Values higher than 4 Mb will be ignored,
  because 4 Mb is the maximum message size Stitch will accept. You can
  tune the amount of time to let pass before triggering a batch
  with ::sc/batch-delay-millis.

  Thread Safety
  -------------

  Instances of StitchClient *are not* thread safe. Concurrent calls to
  `push` will result in lost or corrupt data.
"
  [client-spec]
  (let [builder (StitchClientBuilder.)]
    (doseq [[k v] client-spec]
      (case k
        ::client-id (.withClientId builder (client-id v))
        ::namespace (.withNamespace builder (-namespace v))
        ::token (.withToken builder (token v))
        ::table-name (.withTableName builder (table-name v))
        ::key-names (.withKeyNames builder ^List (key-names v))
        ::batch-size-bytes (.withBatchSizeBytes builder (batch-size-bytes v))
        ::batch-delay-millis (.withBatchDelayMillis builder (batch-delay-millis v))
        :else (throw (IllegalArgumentException. (str "Illegal key" k)))))
    (.build builder)))


(defn push
  "Adds the given message to the current batch, sending it if it is ready.

  client must be an instance of StitchClient, obtained with the `client` function.

  message must be a map with the following structure:

  {::sc/action ::sc/upsert
   ::sc/table-name \"my_table\"
   ::sc/key-names [\"hostname\" \"timestamp\"]
   ::sc/sequence 123456789
   ::sc/data {\"id\" 1, \"name\" \"John\"}}
 "
  [^StitchClient client message]
  (let [sm (StitchMessage.)]
    (doseq [[k v] message]
      (case k
        ::action (.withAction sm (action v))
        ::table-name (.withTableName sm (table-name v))
        ::table-version (.withTableVersion sm (table-version v))
        ::key-names (.withKeyNames sm ^List (key-names v))
        ::sequence (.withSequence sm (-sequence v))
        ::data (.withData sm v)
        :else (throw (IllegalArgumentException. (str "Illegal key" k)))))
    (.push client sm)))
