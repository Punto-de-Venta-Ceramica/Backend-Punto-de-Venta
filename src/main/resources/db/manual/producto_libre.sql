-- Cobro libre ("Artículo artesanal"): marca el producto cuyo precio y nombre se capturan en cada venta.
-- Idempotente. Aplicar antes de desplegar si la base se administra sin ddl-auto=update.
ALTER TABLE barrocontrol.producto
    ADD COLUMN IF NOT EXISTS precio_variable boolean NOT NULL DEFAULT false;
