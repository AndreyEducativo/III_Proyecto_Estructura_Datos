# Contratos compartidos de Social UNA

## MainWindowFactory
La clase `MainWindowFactory` define la creación de la ventana principal del sistema. Sebastián debe implementar esta interfaz en su módulo para reemplazar el placeholder.

## AuditLogger
El logger de auditoría aceptará únicamente `log(AuditLevel, String actor, String accion, String detalle)`. No debe existir una API de edición ni de borrado.

## TextFileStore
Se utiliza para serializar y deserializar archivos de texto con escape de `|`, `\` y saltos de línea.

## SessionContext
El contexto de sesión se usa para compartir el usuario autenticado entre ventanas sin acoplar la UI a la implementación concreta del catálogo.

## UserDirectory
Este contrato expone solo lectura del catálogo: buscar por id, por usuario y listar.

## Jerarquía de excepciones
Se usan `SocialUnaException` y las subclases `ValidationException`, `AuthenticationException`, `DuplicateUserException`, `PersistenceException` y `DataCorruptionException`.

## Integración de Sebastián
Sebastián debe implementar su `MainWindowFactory` y registrarlo en `Main` antes de mostrarse la pantalla principal. Sus archivos de amistad y publicaciones respetarán la convención: `data/amistades.txt` y `data/publicaciones.txt`, con IDs referenciados a los usuarios de `data/usuarios.txt`.
