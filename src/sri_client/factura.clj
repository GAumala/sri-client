(ns sri-client.factura
  (:require [clojure.java.io :as io]
            [com.gaumala.sri.clave-acceso :refer [gen-clave-acceso]]
            [com.gaumala.sri.encoders :as encoders]
            [com.gaumala.sri.xades-bes :refer [sign-comprobante]]
            [sri-client.registry]))

(def IVA_PERCENT_INT 15)
(def IVA_PERCENT_CODE "4") ; Codigo 4 para IVA 15%
(def IVA_DECIMAL 0.15M)

(defn- calculate-total [items]
  (let [subtotal (->> items
                      (map #(* (:cantidad %)
                               (:precio %)))
                      (reduce +))
        iva-total (* subtotal IVA_DECIMAL)
        total (+ subtotal iva-total)]
    {:subtotal (format "%.2f" subtotal)
     :iva-total (format "%.2f" iva-total)
     :total (format "%.2f" total)}))

(defn- item->detalle [item]
  (let [importe-dec (* (:cantidad item)
                       (:precio item))
        importe-str (format "%.2f" importe-dec)]
    {:codigoPrincipal (:codigo item)
     :descripcion (:descripcion item)
     :cantidad (format "%.2f" (bigdec (:cantidad item)))
     :precioUnitario (format "%.2f" (:precio item))
     :precioTotalSinImpuesto importe-str
     :descuento "0.00"
     :impuestos [{:codigo "2" ; IVA
                  :codigoPorcentaje IVA_PERCENT_CODE
                  :baseImponible importe-str
                  :valor (format "%.2f" (* importe-dec IVA_DECIMAL))
                  :tarifa IVA_PERCENT_INT}]}))

(defn- gen-formaPago [forma]
  (condp = forma
    :efectivo "01" ; sin utilizar sist. financiero
    :transferencia "20" ; otros utilizando sist. financiero
    (throw (Exception. (str "Forma de pago desconocida: "
                            forma)))))

(defn- gen-tipoIdentificacionComprador [tipo]
  (condp = tipo
    :cedula "05"
    :ruc "04"

    (throw (Exception. (str "Tipo de identificación desconocido: "
                            tipo)))))

(defn- validate-cliente-id [{:keys [id tipo-id]}]
  (condp = tipo-id
    :cedula (when-not (= 10 (count id))
              (throw (Exception. (str "Cedula debe de tener 10 dígitos: "
                                      id))))
    :ruc (when-not (= 13 (count id))
           (throw (Exception. (str "RUC debe de tener 13 dígitos: "
                                   id))))

    (throw (Exception. (str "Tipo de identificación desconocido: "
                            tipo-id)))))

(defn- gen-infoAdicional [cliente]
  (let [reducer (fn [col [k v]] (condp = k
                                  :email (conj col
                                               {:nombre "Email"
                                                :texto v})
                                  :telefono (conj col
                                                  {:nombre "Telefono"
                                                   :texto v})
                                  col))]
    (reduce reducer [] cliente)))

(defn gen-factura-row
  "Genera una fila para la tabla de facturas, incluyendo la clave de
  acceso y el XML del comprobante electrónico a envíar al SRI. Las 
  facturas generada con esta función tienen las siguentes limitaciones:
  
  - No hay descuentos
  - No hay propinas
  - Pagos en dólares únicamente
  - Pagos sin plazo
  - Pagos sin utilizar el sistema financiero únicamente
  - Todos los productos tienen IVA 15% y no hay más impuestos
  
  Retorna un mapa con todos los keys
  - :comprobante Un string xml de la factura
  - :signed Un string xml de la factura **firmada**
  
  Ejemplo:
  
  ``` clojure:
  (def config {:contribuyente {:razon-social \"Jorge Vera\"
                               :ruc \"0933355555001\"
                               :estab \"001\"
                               :pto-emi \"001\"
                               :dir-matriz \"Alborada\"
                               }
               :keystore {:path \"/path/to/keystore.p12\"
                          :pass \"my_ks_pass123\"}
               :ambiente 1
               :code-8-digitos \"11110000\"})
  (def params {:fecha \"24/04/2025\"
               :codigo \"00000001\"
               :secuencial 1
               :cliente {:razon-social \"Pedro Gómez\"
                         :tipo-id :cedula
                         :id \"0912255555\"
                         :direccion \"Samanes\"
                         :email \"pedro@gmail.com\"
                         :telefono \"2545678\"}
               :items [{:codigo \"PROF-001\"
                        :descripcion \"Servicios Profesionales\"
                        :cantidad 1
                        :precio 100.00M}]})

  (gen-factura-row config params)
  ; => {:ruc \"0933355555001\"
  ;     :estab \"001\"
  ;     :pto-emi \"001\"
  ;     :secuencial 1
  ;     :ambiente 1
  ;     :clave-acceso \"2404202501093335555500110010010000000011111000015\"
  ;     :xml \"<factura id=\\\"comprobante\\\" version=\\\"1.0.0\\\">...\"
  ;     :estado \"pendiente\"}
  ```"
  [{:keys [contribuyente keystore code-8-digitos ambiente]}
   {:keys [cliente forma-pago fecha items secuencial]}]
  (validate-cliente-id cliente)
  (let [clave-acceso (-> {:fechaEmision fecha
                          :ruc (:ruc contribuyente)
                          :ambiente (str ambiente)
                          :codDoc "01" ; factura
                          :estab (:estab contribuyente)
                          :ptoEmi (:pto-emi contribuyente)
                          :secuencial (format "%09d" secuencial)
                          :codigoNumerico code-8-digitos}
                         gen-clave-acceso)
        info-tributaria {:ambiente (str ambiente)
                         :razonSocial (:razon-social contribuyente)
                         :ruc (:ruc contribuyente)
                         :codDoc "01" ; factura
                         :secuencial (format "%09d" secuencial)
                         :dirMatriz (:dir-matriz contribuyente)
                         :estab (:estab contribuyente)
                         :ptoEmi (:pto-emi contribuyente)
                         :claveAcceso clave-acceso}
        {:keys [subtotal iva-total total]} (calculate-total items)
        pagos [{:formaPago (gen-formaPago forma-pago)
                :total total}]
        impuestos [{:codigo "2" ;iva
                    :codigoPorcentaje IVA_PERCENT_CODE
                    :baseImponible subtotal
                    :valor iva-total}]
        info-factura {:fechaEmision fecha
                      :razonSocialComprador (:razon-social cliente)
                      :tipoIdentificacionComprador
                      (gen-tipoIdentificacionComprador (:tipo-id cliente))
                      :identificacionComprador (:id cliente)
                      :direccionComprador (:direccion cliente)
                      :totalSinImpuestos subtotal
                      :totalDescuento "0.00"
                      :importeTotal total
                      :pagos pagos
                      :totalConImpuestos impuestos
                      :moneda "DOLAR"
                      :propina "0.00"}
        info-adicional (gen-infoAdicional cliente)
        detalles (map item->detalle items)
        factura (encoders/factura {:infoTributaria info-tributaria
                                   :infoFactura info-factura
                                   :detalles detalles
                                   :infoAdicional info-adicional})
        xml (->> {:stream (io/input-stream (:path keystore))
                  :pass (:pass keystore)}
                 (sign-comprobante factura))]
    (-> contribuyente
        (select-keys [:ruc :estab :pto-emi])
        (conj {:clave-acceso clave-acceso
               :xml xml
               :secuencial secuencial
               :estado "pendiente"
               :ambiente ambiente}))))
