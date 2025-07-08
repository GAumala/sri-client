CREATE TABLE facturas (
  id SERIAL PRIMARY KEY,
  ruc VARCHAR(13) NOT NULL,
  estab CHAR(3) NOT NULL,
  pto_emi CHAR(3) NOT NULL,
  secuencial INT NOT NULL,
  ambiente INT NOT NULL,
  clave_acceso VARCHAR(49) NOT NULL,
  autorizacion VARCHAR(64),
  content CLOB NOT NULL,
  xml CLOB NOT NULL,
  estado VARCHAR(16) NOT NULL
);

CREATE UNIQUE INDEX idx_facturas ON facturas(ambiente, ruc, estab, pto_emi, secuencial);
