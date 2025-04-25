(ns sri-client.registry
  (:require [clojure.spec.alpha :as sp]
            ;; require specs :sri.comprobantes/*
            [com.gaumala.sri.comprobantes]
            [com.gaumala.sri.predicates :refer [code-8-digits?
                                                some-string?
                                                fechaEmision?]]
            [sri-client.predicates :refer [email? phone-number?]]))

(sp/def :sri-client/ambiente #{1 2})
(sp/def :sri-client/fecha :sri.comprobantes/fechaEmision)
(sp/def :sri-client/secuencial int?)
(sp/def :sri-client/forma-pago #{:efectivo :transferencia})
(sp/def :sri-client.keystore/path some-string?)
(sp/def :sri-client.keystore/pass some-string?)

(sp/def :sri-client/keystore
  (sp/keys :req-un [:sri-client.keystore/path
                    :sri-client.keystore/pass]))

(sp/def :sri-client.contribuyente/razon-social
  :sri.comprobantes/razonSocial)
(sp/def :sri-client.contribuyente/ruc
  :sri.comprobantes/ruc)
(sp/def :sri-client.contribuyente/estab
  :sri.comprobantes/estab)
(sp/def :sri-client.contribuyente/pto-emi
  :sri.comprobantes/ptoEmi)
(sp/def :sri-client.contribuyente/dir-matriz
  :sri.comprobantes/dirMatriz)
(sp/def :sri-client.contribuyente/dir-estab
  :sri.comprobantes/dirEstablecimiento)

(sp/def :sri-client/contribuyente
  (sp/keys :req-un [:sri-client.contribuyente/razon-social
                    :sri-client.contribuyente/ruc
                    :sri-client.contribuyente/estab
                    :sri-client.contribuyente/pto-emi
                    :sri-client.contribuyente/dir-matriz]
           :opt-un [:sri.comprobantes/dirEstablecimiento]))

(sp/def :sri-client.cliente/razon-social
  :sri.comprobantes/razonSocialComprador)
(sp/def :sri-client.cliente/tipo-id
  #{:ruc :cedula})
(sp/def :sri-client.cliente/id
  :sri.comprobantes/identificacionComprador)
(sp/def :sri-client.cliente/direccion
  :sri.comprobantes/direccionComprador)
(sp/def :sri-client.cliente/email email?)
(sp/def :sri-client.cliente/telefono phone-number?)

(sp/def :sri-client/cliente
  (sp/keys :req-un [:sri-client.cliente/razon-social
                    :sri-client.cliente/tipo-identificacion
                    :sri-client.cliente/identificacion
                    :sri-client.cliente/direccion]
           :opt-un [:sri-client/email
                    :sri-client.cliente/telefono]))

(sp/def :sri-client.item/codigo
  :sri.comprobantes.detalle/codigoPrincipal)
(sp/def :sri-client.item/descripcion
  :sri.comprobantes.detalle/descripcion)
(sp/def :sri-client.item/cantidad int?)
(sp/def :sri-client.item/precio decimal?)

(sp/def :sri-client/item
  (sp/keys :req-un [:sri-client.item/codigo
                    :sri-client.item/descripcion
                    :sri-client.item/cantidad
                    :sri-client.item/precio]))

(sp/def :sri-client/items (sp/+ :sri-client/item))

(sp/def :sri-client.factura/id integer?)
(sp/def :sri-client.factura/xml some-string?)
(sp/def :sri-client.factura/ambiente :sri-client/ambiente)
(sp/def :sri-client.factura/clave-acceso :sri.comprobantes/claveAcceso)
