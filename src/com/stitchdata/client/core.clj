(ns com.stitchdata.client.core
  (:import [com.stitchdata.client StitchClientBuilder
            StitchClient StitchMessage StitchMessage$Action]
           [java.util List]))

(set! *warn-on-reflection* true)

(defn- action [action-kw]
  (case action-kw
    ::upsert      StitchMessage$Action/UPSERT
    ::switch-view StitchMessage$Action/SWITCH_VIEW
    :else
    (throw (IllegalArgumentException.
            (str "action must be either " ::upsert " or " ::switch-view)))))

(defn client-id [x]
  (when-not (integer? x)
    (throw (IllegalArgumentException. "client-id must be an integer")))
  x)

(defn -namespace [x]
  (when-not (string? x)
    (throw (IllegalArgumentException. "namespace must be a string")))
  x)

(defn token [x]
  (when-not (string? x)
    (throw (IllegalArgumentException. "token must be a string")))
  x)

(defn table-name [x]
  (when-not (string? x)
    (throw (IllegalArgumentException. "table-name must be a string")))
  x)

(defn table-version [x]
  (when-not (integer? x)
    (throw (IllegalArgumentException. "table-version must be an integer")))
  x)

(defn key-names [x]
  (when-not (coll? x)
    (throw (IllegalArgumentException. "key-names must be a collection")))
  x)

(defn batch-size-bytes [x]
  (when-not (integer? x)
    (throw (IllegalArgumentException. "batch-size-bytes must be an integer")))
  x)

(defn batch-delay-millis [x]
  (when-not (integer? x)
    (throw (IllegalArgumentException. "batch-delay-millis must be an integer")))
  x)

(defn -sequence [x]
  (when-not (integer? x)
    (throw (IllegalArgumentException. "sequence must be an integer")))
  x)

(defn client [client-spec]
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
