-- Solo para un esquema revisado. No se ejecuta al iniciar el backend.
-- En bases con datos, los precios y el negocio de cada área deben decidirse
-- explícitamente: no se inventan valores para registros existentes.
BEGIN;
DO $$
BEGIN
    IF to_regclass('barrocontrol.area') IS NULL OR to_regclass('barrocontrol.producto') IS NULL THEN
        RAISE EXCEPTION 'Faltan las tablas area o producto; revise el esquema antes de continuar';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='barrocontrol' AND table_name='area' AND column_name='id_negocio')
       AND EXISTS (SELECT 1 FROM barrocontrol.area) THEN
        RAISE EXCEPTION 'Hay áreas sin negocio: asigne y valide su negocio antes de agregar id_negocio obligatorio';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='barrocontrol' AND table_name='producto' AND column_name='precio')
       AND EXISTS (SELECT 1 FROM barrocontrol.producto) THEN
        RAISE EXCEPTION 'Hay productos sin precio: determine los precios antes de agregar la columna obligatoria';
    END IF;
END $$;
ALTER TABLE barrocontrol.area ADD COLUMN IF NOT EXISTS id_negocio BIGINT NOT NULL REFERENCES barrocontrol.negocio(id);
CREATE INDEX IF NOT EXISTS ix_area_negocio ON barrocontrol.area(id_negocio);
ALTER TABLE barrocontrol.producto ADD COLUMN IF NOT EXISTS precio NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (precio >= 0);
ALTER TABLE barrocontrol.producto ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
COMMIT;
