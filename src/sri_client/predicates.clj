(ns sri-client.predicates)

(defn input-stream? [x]
  (instance? java.io.InputStream x))

(defn email? [input]
  (and (string? input)
       (re-matches #"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}" input)))

(defn phone-number? [input]
  (and (string? input)
       (re-matches #"\d{6,15}" input)))
