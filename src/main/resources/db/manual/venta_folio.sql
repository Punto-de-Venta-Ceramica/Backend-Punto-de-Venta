-- Ejecutar en bases existentes antes de iniciar la versión con folio de venta.
-- Mismas reglas que pedido.folio: varchar(50), obligatorio, sin espacios en blanco y único por sucursal
-- (cada sucursal tiene una sola caja, así que basta con que sea único por caja).
BEGIN;

ALTER TABLE barrocontrol.venta
    ADD COLUMN IF NOT EXISTS folio varchar(50);

-- Las ventas anteriores reciben el folio con el mismo formato que las nuevas: VTA-000123.
UPDATE barrocontrol.venta
SET folio = 'VTA-' || lpad(id::text, 6, '0')
WHERE folio IS NULL;

ALTER TABLE barrocontrol.venta
    ALTER COLUMN folio SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_venta_2' AND conrelid = 'barrocontrol.venta'::regclass
    ) THEN
        ALTER TABLE barrocontrol.venta
            ADD CONSTRAINT ck_venta_2 CHECK (btrim(folio) <> '');
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_venta_folio_caja' AND conrelid = 'barrocontrol.venta'::regclass
    ) THEN
        ALTER TABLE barrocontrol.venta
            ADD CONSTRAINT uq_venta_folio_caja UNIQUE (id_caja, folio);
    END IF;
END
$$;

COMMIT;
