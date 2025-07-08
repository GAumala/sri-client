(ns sri-client.ride
  (:require [clj-pdf.core :as pdf]
            [clojure.string :as s]))

(defn- adicionales-lines [adicionales]
  (when adicionales
    (s/join (map #(str "\n" (:nombre %) ": " (:valor %)) adicionales))))

(defn- find-iva-15 [factura]
  (let [impuestos (get-in factura [:content :infoFactura :totalConImpuestos])
        matches (filter #(and (= (:codigo %) "2")
                              (= (:codigoPorcentaje %) "4")) impuestos)
        target (first matches)]
    (if target
      (:valor target)
      "0.00")))

(defn from-factura [factura filename]
  (pdf/pdf
   [{:title "RIDE - Factura Electrónica"
     :footer {:text "Este documento es la representación impresa de un comprobante electrónico autorizado por el SRI."
              :align :center
              :page-numbers false}
     :font {:size 10}
     :left-margin 20
     :right-margin 20
     :top-margin 20
     :bottom-margin 20
     :size :a4}
    [:heading {:style {:align :center}} "RIDE (Representación Impresa del Comprobante Electrónico)"]
    [:line]
    [:table {:width 70
             :widths [25 75]
             :spacing 0
             :padding 0
             :border false
             :cell-border false}
     [[:cell [:phrase {:style :bold} "RUC:"]]
      (get-in factura [:content :infoTributaria :ruc])]
     [[:cell [:phrase {:style :bold} "Razón Social:"]]
      (get-in factura [:content :infoTributaria :razonSocial])]
     [[:cell [:phrase {:style :bold} "Dirección Matriz:"]]
      (get-in factura [:content :infoTributaria :dirMatriz])]
     [[:cell [:phrase {:style :bold} "Clave de Acceso:"]]
      (get-in factura [:content :infoTributaria :claveAcceso])]
     [[:cell [:phrase {:style :bold} "Nº de Autorización:"]]
      (or (:autorizacion factura)
          "-")]
     [[:cell [:phrase {:style :bold} "Fecha de emisión:"]]
      (get-in factura [:content :infoFactura :fechaEmision])]]
    [:spacer]
    [:table {:width 50
             :widths [30 70]
             :spacing 0
             :padding 0
             :border false
             :cell-border false}
     [[:cell {:colspan 2}
       [:phrase {:style :bold :size 12} "Datos del Cliente"]]]
     [[:cell [:phrase {:style :bold} "RUC:"]]
      (get-in factura [:content :infoFactura :identificacionComprador])]
     [[:cell [:phrase {:style :bold} "Razón Social:"]]
      (get-in factura [:content :infoFactura :razonSocialComprador])]
     [[:cell [:phrase {:style :bold} "Dirección:"]]
      (get-in factura [:content :infoFactura :direccionComprador])]]
    [:spacer]
    (vec (concat [:table {:header ["Cantidad" "Descripción" "Precio" "Total"]
                          :widths [10 50 20 20]
                          :spacing 0
                          :padding 0
                          :border false
                          :cell-border false}]
                 (mapv (fn [item]
                         [(:cantidad item)
                          (str (:descripcion item)
                               (adicionales-lines (:detallesAdicionales item)))
                          (str "$" (:precioUnitario item))
                          (str "$" (:precioTotalSinImpuesto item))])
                       (get-in factura [:content :detalles]))))
    [:table {:width 30
             :widths [35 65]
             :spacing 0
             :padding 0
             :border false
             :cell-border false}
     [[:cell [:phrase {:style :bold} "Subtotal:"]]
      (str "$ " (get-in factura [:content :infoFactura :totalSinImpuestos]))]
     [[:cell [:phrase {:style :bold} "IVA (15%):"]]
      (str "$ " (find-iva-15 factura))]
     [[:cell [:phrase {:style :bold} "TOTAL:"]]
      (str "$ " (get-in factura [:content :infoFactura :importeTotal]))]]]
   filename))
