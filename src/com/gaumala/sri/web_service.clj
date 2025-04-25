(ns com.gaumala.sri.web-service
  (:require [com.gaumala.sri.decoders :as decoders]
            [com.gaumala.sri.encoders :as encoders]
            [org.httpkit.client :as client]))

(def BASE_URL_TEST "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/")
(def PRUEBAS_URLS
  {:autorizacion (str BASE_URL_TEST "AutorizacionComprobantesOffline?wsdl")
   :recepcion (str BASE_URL_TEST "RecepcionComprobantesOffline?wsdl")})

(def BASE_URL_PROD "https://cel.sri.gob.ec/comprobantes-electronicos-ws/")
(def PROD_URLS
  {:autorizacion (str BASE_URL_PROD "AutorizacionComprobantesOffline?wsdl")
   :recepcion (str BASE_URL_PROD "RecepcionComprobantesOffline?wsdl")})

(defn- get-url [ambiente url-key]
  (condp = ambiente
    1 (get PRUEBAS_URLS url-key)
    2 (get PROD_URLS url-key)
    (throw (Exception. (str "ambiente desconocido: " ambiente)))))

(defn validar-comprobante [ambiente xml-string]
  (let [url (get-url ambiente :recepcion)
        req-body (encoders/validar-comprobante xml-string)
        response @(client/post url
                               {:body req-body
                                :as :text})
        res-text (:body response)
        decoded (decoders/respuesta-recepcion-comprobante res-text)]
    (if (nil? decoded)
      (throw (Exception. (str "Error de request validar-comprobante. "
                              "status: " (:status response) "\n\n"
                              res-text)))
      decoded)))

(defn autorizacion-comprobante [ambiente clave-acceso]
  (let [url (get-url ambiente :autorizacion)
        req-body (encoders/autorizacion-comprobante clave-acceso)
        response @(client/post url
                               {:body req-body
                                :as :text})
        res-text (:body response)
        decoded (decoders/respuesta-autorizacion-comprobante res-text)]
    (if (nil? decoded)
      (throw (Exception. (str "Error de request autorizacion-comprobante. "
                              "status: " (:status response) "\n\n"
                              res-text)))
      decoded)))
