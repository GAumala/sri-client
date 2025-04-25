(ns sri-client
  (:require [clojure.spec.alpha :as sp]
            [com.gaumala.sri.web-service :as ws]
            [com.gaumala.utils.spec :as spec-utils]
            [sri-client.config :refer [config]]
            [sri-client.db :as db]
            [sri-client.factura :refer [gen-factura-row]]
            [sri-client.registry]))

(defn- get-next-secuencial [{:keys [contribuyente secuencial-1]}
                            new-factura]
  (or ; si se especifica secuencial usar ese
   (:secuencial new-factura)
   ; sino buscar en base de datos el ultimo
   ; y hacer +1
   (some->> contribuyente
            db/get-last-secuencial
            inc)
   ; si base de datos vacia usar el primer 
   ; secuencial especificado en la configuración 
   secuencial-1))

(sp/def :sri-client.new-factura/params
  (sp/keys :req-un [:sri-client/cliente
                    :sri-client/fecha
                    :sri-client/forma-pago
                    :sri-client/items]
           :opt-un [:sri-client/secuencial]))

(defn new-factura [params]
  {:pre (spec-utils/validate :sri-client.new-factura/params params)}
  (let [secuencial (get-next-secuencial config params)
        row (->> (assoc params :secuencial secuencial)
                 (gen-factura-row config))
        id (db/insert-factura row)]
    (assoc row :id id)))

(defn- print-mensajes [mensajes]
  (doseq [{:keys [identificador
                  mensaje
                  tipo
                  informacionAdicional]} mensajes]
    (println (str tipo ": " mensaje " id=" identificador
                  "\n" informacionAdicional))))

(sp/def :sri-client.submit-factura/params
  (sp/keys :req-un [:sri-client.factura/id
                    :sri-client.factura/ambiente
                    :sri-client.factura/xml]))

(defn submit-factura [{:keys [id ambiente xml] :as params}]
  {:pre (spec-utils/validate :sri-client.submit-factura/params params)}
  (let [res (ws/validar-comprobante ambiente xml)]
    (condp = (:estado res)
      "RECIBIDA" (do (db/update-factura-estado {:id id
                                                :estado "recibida"})
                     (println "Factura recibida por el SRI"))
      "DEVUELTA" (do (db/update-factura-estado {:id id
                                                :estado "rechazada"})
                     (println "Factura rechazada por el SRI\n")
                     (print-mensajes (:mensajes res)))
      (println "ERROR: resupuesta desconocida"))
    res))

(sp/def :sri-client.consult-factura/params
  (sp/keys :req-un [:sri-client.factura/id
                    :sri-client.factura/ambiente
                    :sri-client.factura/clave-acceso]))

(defn consult-factura [{:keys [id ambiente clave-acceso] :as params}]
  {:pre (spec-utils/validate :sri-client.consult-factura/params params)}
  (let [res (ws/autorizacion-comprobante ambiente clave-acceso)
        autorizacion (first (:autorizaciones res))]
    (condp = (:estado autorizacion)
      "AUTORIZADO" (do (db/update-factura-estado {:id id
                                                  :estado "autorizada"})
                       (println "Factura autorizada por el SRI"))
      "NO AUTORIZADO" (do (db/update-factura-estado {:id id
                                                     :estado "rechazada"})
                          (println "Factura NO autorizada por el SRI")
                          (print-mensajes (:mensajes autorizacion)))
      "RECHAZADA" (do (db/update-factura-estado {:id id
                                                 :estado "rechazada"})
                      (println "Factura rechazada por el SRI")
                      (print-mensajes (:mensajes autorizacion)))
      (println "ERROR: resupuesta desconocida"))
    res))
