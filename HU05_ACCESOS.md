# HU-05: accesos y permisos del backend

Las autoridades se consultan en la base de datos en cada solicitud autenticada.
Si el usuario fue eliminado o su rol ya no coincide con el de su JWT, la API
rechaza la sesión con 401; los permisos se obtienen de los valores vigentes y no
del JWT. Al cambiar de rol se requiere iniciar sesión de nuevo.

| Función | Propietario | Empleado |
| --- | --- | --- |
| Registrar un pedido en sucursal autorizada | Sí | Sí, en la asignada |
| Consultar y registrar pagos de un pedido autorizado | Sí | Sí, en la asignada |
| Pasar un pedido a producción o cancelarlo | Sí | No |
| Editar descripción, entrega y datos del cliente de un pedido | Sí | No |
| Registrar productos | Sí, en sus sucursales | Sí, en la asignada |
| Editar o eliminar productos y ajustar inventario | Sí | No |
| Administrar negocios, sucursales, áreas, categorías y empleados | Sí | No |
| Reportes o contabilidad de negocio | Sí | No |

Para capturar un producto, `GET /api/productos/opciones/categorias?sucursalId=...`
devuelve categorías activas del negocio de una sucursal autorizada. La creación
valida por separado que las existencias iniciales correspondan a sucursales donde
el usuario puede registrar productos.

`PUT /api/pedidos/{id}/datos` recibe `descripcion`, `tipoEntrega`,
`descripcionEnvio` y `cliente` con nombre, teléfono, ciudad, colonia y calle.
Solo el propietario del negocio puede usarlo. Guarda los datos del cliente para
ese pedido sin alterar otros pedidos que pudieran compartir el cliente anterior;
no modifica las líneas de productos ni los pagos existentes. Los pedidos
cancelados no se pueden editar con esta operación.

El empleado puede ver los importes y el saldo de cada pedido de su sucursal para
registrar cobros. La decisión no autoriza informes financieros generales.
Las rutas `/api/reportes/**` y `/api/contabilidad/**` están reservadas al
propietario; no sustituyen la revisión de autorización de cualquier ruta nueva.

La HU-08 de ventas, los reportes financieros y una operación para modificar los
artículos de pedidos aún no están implementados en esta rama. Sus permisos
deben verificarse cuando existan esos casos de uso. No se agregó un endpoint
ficticio de ventas ni se alteró el esquema de la base de datos.
