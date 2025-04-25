(ns sri-client.config
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as sp]
            [com.gaumala.sri.predicates :refer [code-8-digits?]]
            [sri-client.registry]))

(def CONFIG_PATH "./config.edn")

(sp/def :sri-client.config/code-8-digitos code-8-digits?)
(sp/def :sri-client.config/secuencial-1 int?)

(sp/def :sri-client/config
  (sp/keys :req-un [:sri-client/keystore
                    :sri-client/contribuyente]
           :opt-un [:sri-client/ambiente
                    :sri-client.config/code-8-digitos
                    :sri-client.config/secuencial-1]))

(def default-config {:ambiente 1
                     :code-8-digitos "11110000"
                     :secuencial-1 1})

(def config
  (try
    (let [content (slurp CONFIG_PATH)
          value (edn/read-string content)]
      (if (sp/valid? :sri-client/config value)
        ;; config es valida
        (conj default-config value)

        ;; config no es valida, mostrar error
        (let [abs-path (-> (java.io.File. CONFIG_PATH)
                           (.getAbsolutePath))
              msg (str "Error al cargar config: " abs-path "\n\n"
                       "Hay un error en tu configuración: "
                       (sp/explain-str :sri-client/config value))]
          (println msg)
          default-config)))
    (catch Exception e
      (let [abs-path (-> (java.io.File. CONFIG_PATH)
                         (.getAbsolutePath))
            msg (str "Error al cargar config: " abs-path "\n\n"
                     "Por favor revisa tu configuración y "
                     "vuelve a cargar el namespace.")]
        (println msg))
      default-config)))

(def ambiente (:ambiente config))
