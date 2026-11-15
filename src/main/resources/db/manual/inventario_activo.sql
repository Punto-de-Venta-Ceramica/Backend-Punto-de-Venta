-- Solo para un esquema revisado. No se ejecuta al iniciar el backend.
-- Baja lógica por sucursal: quitar un producto de una sucursal pone activo = false
-- y conserva su existencia. Los registros existentes quedan activos.
BEGIN;
DO $$
BEGIN
    IF to_regclass('barrocontrol.inventario') IS NULL THEN
        RAISE EXCEPTION 'Falta la tabla inventario; revise el esquema antes de continuar';
    END IF;
END $$;
ALTER TABLE barrocontrol.inventario ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
COMMIT;
