(ns sri-client.db
  (:require [clojure.java.io :refer [resource]]
            [clojure.set :refer [rename-keys]]
            [next.jdbc.result-set :refer [as-maps-adapter
                                          clob-column-reader
                                          as-unqualified-lower-maps]]
            [next.jdbc.sql :refer [insert!
                                   delete!
                                   update!
                                   get-by-id]]
            [next.jdbc :as jdbc]))

(def clob-builder
  (as-maps-adapter
   as-unqualified-lower-maps
   clob-column-reader))

(def ds (jdbc/get-datasource {:jdbcUrl "jdbc:h2:./.db/sri_client"}))

(defn initialize []
  (let [statements (slurp (resource "tables.sql"))]
    (jdbc/execute! ds [statements])))

(defn get-last-secuencial [{:keys [ruc estab pto-emi]}]
  (let [stmt (str "SELECT secuencial FROM facturas "
                  "WHERE ruc = ? AND estab = ? AND pto_emi = ? "
                  "ORDER BY secuencial DESC LIMIT 1")
        opts {:builder-fn as-unqualified-lower-maps}
        rows (jdbc/execute! ds [stmt ruc estab pto-emi] opts)]
    (-> rows first :secuencial)))

(defn- translate-to-db-keys [row]
  (rename-keys row {:pto-emi :pto_emi
                    :clave-acceso :clave_acceso}))

(defn- translate-from-db-keys [row]
  (rename-keys row {:pto_emi :pto-emi
                    :clave_acceso :clave-acceso}))

(defn insert-factura [params]
  (let [new-factura (-> params
                        (select-keys [:ruc
                                      :estab
                                      :pto-emi
                                      :secuencial
                                      :ambiente
                                      :clave-acceso
                                      :estado
                                      :content
                                      :xml])
                        (translate-to-db-keys)
                        (update :content pr-str))
        row (insert! ds :facturas new-factura)]
    (:FACTURAS/ID row)))

(defn update-factura-estado [{:keys [id estado autorizacion]}]
  (let [edits (if autorizacion
                {:estado estado
                 :autorizacion autorizacion}
                {:estado estado})]
    (update! ds :facturas edits {:id id})))

(defn list-facturas
  ([] (list-facturas 10))
  ([limit]
   (let [columns "id, ambiente, ruc, estab, pto_emi, secuencial, estado"
         stmt (str "SELECT " columns
                   " FROM facturas ORDER BY id DESC LIMIT ?")
         opts {:builder-fn as-unqualified-lower-maps}
         rows (jdbc/execute! ds [stmt (or limit 10)] opts)]
     (map translate-from-db-keys rows))))

(defn get-factura-data [id]
  (let [opts {:builder-fn clob-builder
              :columns [:id :ambiente :clave_acceso :xml :content]}]
    (some-> (get-by-id ds :facturas id opts)
            (translate-from-db-keys)
            (update :content read-string))))

(defn delete-factura [id]
  (delete! ds :facturas {:id id}))
