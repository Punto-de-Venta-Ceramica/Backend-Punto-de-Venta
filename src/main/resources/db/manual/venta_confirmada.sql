-- Ejecutar en bases existentes antes de iniciar la versión con confirmación de ventas.
BEGIN;

ALTER TABLE barrocontrol.venta
    ADD COLUMN IF NOT EXISTS monto_recibido numeric(18,2),
    ADD COLUMN IF NOT EXISTS clave_operacion uuid;

ALTER TABLE barrocontrol.detalle_venta
    ADD COLUMN IF NOT EXISTS nombre_producto varchar(150);

CREATE UNIQUE INDEX IF NOT EXISTS uq_venta_caja_operacion
    ON barrocontrol.venta (id_caja, clave_operacion);

COMMIT;
