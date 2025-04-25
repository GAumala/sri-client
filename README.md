# sri-client

Facturación en REPL

Este programa es un ejemplo de como usar la [librería SRI](https://github.com/GAumala/SRI)

Cada factura generada se guarda en una base datos local (H2) antes de
enviarse al SRI.

La base datos se usa para calcular el secuencial correspondiente y registrar el estado de los comprobantes (*AUTORIZADO, NO AUTORIZADO*).

### Setup

Primero crea un archivo de configuración `./config.edn` especificando el ambiente, el keystore de la firma eléctronica y los datos del contribuyente:

```clojure
{; keys requeridos
 :contribuyente {:razon-social "Jorge Vera"
                 :ruc "0953234765001"
                 :estab "001"
                 :pto-emi "001"
                 :dir-matriz "Alborada"}
 :keystore {:path "/path/to/my/keystore.p12"
            :pass "<KEYSTORE_PASSWORD>"}

 ; keys opcionales
 :ambiente 1 ; 1 para pruebas 2 para producción (default=1)
 :code-8-digitos "10101010" ; codigo para clave de acceso (default="11110000")
 :secuencial-1 9 ; secuencial a usar cuando base de datos está vacía (default=1)
}
```

Luego en un REPL inicializa la base de datos

```clojure
(require 'sri-client)
(in-ns 'sri-client)

(db/initialize)
```

### Uso

En un REPL usa el namespace `sri-client` para crear facturas

```clojure
(require 'sri-client)
(in-ns 'sri-client)

; crea una factura en la base de datos
(def latest
  (new-factura {:fecha "24/04/2025"
                :forma-pago :transferencia
                :cliente {:razon-social "Pedro Gómez"
                          :tipo-id :cedula
                          :id "0912255555"
                          :direccion "Samanes"
                          :telefono "2545678"
                          :email "pedro@gmail.com"}
                :items [{:codigo "P-001"
                         :descripcion "Prueba"
                         :cantidad 1
                         :precio 1.00M}]}))

; escribir el XML a un archivo factura.xml para revisarlo
; en el navegador
(spit "factura.xml" (:xml latest))

; enviar factura al SRI
(def resp (submit-factura latest))

; consultar estado de autorizacion con el SRI
(def resp (consult-factura latest))

; mostrar facturas en base datos
(db/list-facturas)

; cargar una factura de la base de datos
(def factura (db/get-factura-data 1))
```
