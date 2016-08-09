Clojure Stitch Client
=====================

Quick Start
-----------

### Require the Library

There's only one namespace to require,
`com.stitchdata.client.core`. Since you'll need to use namespaced
keywords, we recommend requiring it with an alias, e.g. `:as sc`.

```clojure
(ns your.namespace
  (:require [com.stitchdata.client.core :as sc]))
```

### Build a Client

Use the `sc/client` function to build an instance of a stitch
client. You'll need to set your client id, authentication token, and
namespace. You should have gotten these when you set up the
integration at http://stitchdata.com. You should close the client when
you're done with it to ensure that all messages are delivered, so we
recommend opening it in a `with-open` form.

```clojure
(with-open [stitch (sc/client {::sc/client-id client-id
                               ::sc/token token
                               ::sc/namespace -namespace})]
  ...)
```

### Sending Messages

You send a message by calling `sc/push` and passing in the client and
the message you want to send:

```clojure
(sc/push stitch message)
```

A message is a map with the following structure:

```clojure
{::sc/action ::sc/upsert
 ::sc/table-name "my_table"
 ::sc/key-names ["id"]
 ::sc/sequence (System/currentTimeMillis)
 ::sc/data data}
```

* `::sc/action` is the action to perform, currently only `::sc/upsert`
* `::sc/table-name` is the name of the table you want to load into
* `::sc/key-names` is a list of primary key column names
* `::sc/sequence` is any arbitrary increasing number used to determine order of updates
* `::sc/data` is the payload

Data must be a map that conforms to the following rules:

* All keys are strings
* All values are one of:
  * Number (Long, Integer, Short, Byte, Double, Float, BigInteger, BigDecimal)
  * String
  * Boolean
  * Date
  * Map (with string keys and values that conform to these rules)
  * Lists (of objects that conform to these rules)
* It must have a non-null value for each of the keys you specified as "key names"

Note that Clojure keywords are _not_ allowed in the data map.

Running the Example Program
---------------------------

Please see [example.clj](src/com/stitchdata/client/example.clj) for a
full working example. You can run it by executing this command
(replacing CLIENT_ID, TOKEN, and NAMESPACE with your own values):

```bash
lein run -m com.stitchdata.client.example CLIENT_ID TOKEN NAMESPACE
```

On a successful run, you'll see a "Sent example records to Stitch"
message. You should then wait a few minutes and check your data
warehouse, and you should see the example records.

Advanced Topics
---------------

### Setting message defaults on the client

In a typical use case, several of the fields will be the same for all
messages that you send using a single client. To make this use case
more convenient, you can set some of those fields on the client. The
resulting client will inject the values for those fields into every
message it sends.

```clojure
(with-open [stitch (sc/client {::sc/client-id (Integer/parseInt client-id)
                               ::sc/token token
                               ::sc/namespace -namespace
                               ::sc/table-name "my-table"
                               ::sc/key-names ["hostname" "timestamp"]})]
  ...
  (sc/push client {::sc/action ::sc/upsert
                   ::sc/sequence (System/currentTimeMillis)
                    ::sc/data data})
  ...)
```

### Tuning Buffer Parameters

By default `sc/push` will accumulate messages locally in a batch, and
then deliver the batch when one of the following conditions is met:

* The batch has 4 Mb of data
* The batch has 10,000 records
* A minute has passed since the last batch was sent.

If you want to send data more frequently, you can lower the buffer
capacity or the time limit.

```clojure
(with-open [stitch (sc/client {::sc/client-id (Integer/parseInt client-id)
                               ::sc/token token
                               ::sc/namespace -namespace

                               ;; Trigger batch at 1 Mb
                               ::sc/batch-size-bytes 1000000

                               ;; Trigger batch after 10 seconds
                               ::sc/batch-delay-millis 10000})]
  ...)
```

Setting the batch size to 0 bytes will effectively turn off batching
and force `push` to send a batch of one record with every call. This
is not generally recommended, as batching will give better
performance, but can be useful for low-volume streams or for
debugging.

There is no value in setting a buffer capacity higher than 4 Mb, since
that is the maximum message size Stitch will accept. If you set it to
a value higher than that, you will use more memory, but StitchClient
will deliver the messages in batches no larger than 4 Mb anyway.

Asynchronous Usage
------------------

StitchClient is *not* thread-safe. Calling any of methods concurrently
can result in lost or corrupt data. If your application has multiple
threads producing data, we recommend using a separate client for each
thread.

License
-------

Copyright © 2016 Stitch

Distributed under the Apache License Version 2.0
