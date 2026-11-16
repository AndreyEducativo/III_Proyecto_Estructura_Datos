# Contratos compartidos entre los módulos de Andrey y Sebastián

Este documento explica qué piezas comparten los dos módulos y cómo quedó integrado el módulo social
(amistades, publicaciones y ventana principal) **sin modificar** el login, el registro, el
controlador de navegación ni los servicios de usuarios.

> Convenciones del proyecto: Java 17, nombres en español (excepto `Main`), comentarios y Javadoc
> en español, archivos en UTF-8, dependencias inyectadas por constructor (nada de singletons).

---

## 1. Piezas compartidas

| Contrato | Paquete | Cómo lo usa el módulo social |
|----------|---------|------------------------------|
| `DirectorioUsuarios` (interfaz, solo lectura) | `una.social.servicio` | `GestorAmistades`, `GestorPublicaciones` y `BuscadorUsuarios` validan IDs y obtienen nombres con `buscarPorId`, `listarTodos`. No pueden modificar usuarios. |
| `Usuario` (inmutable) | `una.social.modelo` | Usuario activo y autores; su `getId()` relaciona amistades y publicaciones. |
| `FabricaVentanaPrincipal` (interfaz) | `una.social.ui` | `Main` registra la `VentanaPrincipal` real (ver punto 3). |
| `RegistroAuditoria` + `NivelAuditoria` | `una.social.auditoria` | Se auditan `AMISTAD_AGREGADA`, `AMISTAD_RECHAZADA`, `PUBLICACION_CREADA`, `PUBLICACION_RECHAZADA`, cargas, guardados y líneas omitidas. |
| `AlmacenArchivoTexto`, `ArchivoRegistros`, `LineaArchivo` | `una.social.persistencia` | Escape de campos, lectura que omite y audita líneas dañadas, escritura atómica con encabezado. |
| `ModuloPersistente` (interfaz) | `una.social.persistencia` | `GestorAmistades` y `GestorPublicaciones` la implementan: se cargan al iniciar y se guardan al cerrar sesión y al cerrar la app. |
| Jerarquía de excepciones | `una.social.excepciones` | Nuevas subclases: `ExcepcionAmistadInvalida`, `ExcepcionPublicacionInvalida`. |
| `TemaUi` y componentes | `una.social.ui.tema` | Botones, campos, tarjetas y colores de la ventana principal. Nuevo: `AreaTextoTema` (cuadro de varias líneas). |
| `Dialogos`, `TareaEnSegundoPlano` | `una.social.ui` | Alertas consistentes y trabajo de archivos fuera del hilo de Swing. |

## 2. Contratos propios del módulo social

| Interfaz | Implementación | La usa |
|----------|----------------|--------|
| `ConsultaAmistades` (`idsAmigos`, `sonAmigos`) | `GestorAmistades` | `GestorPublicaciones` (muro) y `BuscadorUsuarios` |
| `ServicioAmistades` (`agregarAmistad`, `listarAmigos`) | `GestorAmistades` | `PanelBuscador`, `PanelAmigos` |
| `ServicioBusqueda` (`buscar`) | `BuscadorUsuarios` | `PanelBuscador` |
| `ServicioPublicaciones` (`publicar`, `obtenerMuro`) | `GestorPublicaciones` | `PanelMuro` |
| `RepositorioAmistades` / `RepositorioPublicaciones` | `...ArchivoTexto` | los gestores |

## 3. Integración en `Main`

1. **Servicios** (`EnsambladorSocial`): `GestorAmistades`, `GestorPublicaciones` y
   `BuscadorUsuarios`, que reciben el `GestorUsuarios` como `DirectorioUsuarios`.
2. **Carga en dos fases**: primero usuarios (y sus datos de prueba si está vacío); después
   amistades y publicaciones, porque validan que los IDs existan. Si los usuarios no se pudieron
   leer, los módulos sociales no se cargan y quedan protegidos contra guardados que borrarían sus
   archivos.
3. **Datos de ejemplo** (`HerramientaDatosSemillaSocial`): solo si existen U001–U003 y nadie tiene
   amigos ni publicaciones.
4. **Guardado**: un `CoordinadorPersistencia` con los tres módulos (usuarios, amistades,
   publicaciones) guarda al cerrar sesión y en el gancho de cierre de la app.
5. **Ventana principal**: una sola línea registra la ventana real:

```java
FabricaVentanaPrincipal fabricaVentanaPrincipal = (usuario, alCerrarSesion) -> new VentanaPrincipal(
        usuario, alCerrarSesion, gestorPublicaciones, gestorAmistades, buscador, auditoria);
```

`VentanaPrincipalTemporal` se conserva en el proyecto como implementación alternativa de la fábrica,
pero ya no se usa.

## 4. Formatos de archivo en `data/`

Convención común: UTF-8, un registro por línea, columnas separadas por `|`, líneas con `#` son
comentarios, escape `\|`, `\\`, `\n`, `\r`, escritura atómica.

| Archivo | Columnas | Reglas al cargar |
|---------|----------|------------------|
| `usuarios.txt` | `id\|nombre\|usuario\|salBase64\|hashBase64\|fechaISO` | ID `U###`, Base64 y fecha válidos, sin usuarios repetidos |
| `amistades.txt` | `idUsuarioA\|idUsuarioB\|fechaISO` | ambos IDs existen, distintos entre sí, par no repetido (en cualquier orden) |
| `publicaciones.txt` | `idPublicacion\|idAutor\|fechaISO\|texto` | ID `P####` no repetido, autor existe, texto no vacío y de máximo 500 caracteres |

Toda línea que no cumple se **omite y se audita** (`LINEA_CORRUPTA_OMITIDA`) y la carga continúa.

## 5. Convención de IDs

- Usuarios: `U` + al menos 3 dígitos (`U001`...). Publicaciones: `P` + al menos 4 dígitos (`P0001`...).
- Ambos se generan como "máximo existente + 1", por eso nunca se repiten aunque haya huecos.
- Usuarios de prueba fijos: **U001 Ana Mora (`ana`), U002 Luis Rojas (`luis`), U003 Maria Vargas
  (`maria`)**.

## 6. Reglas de auditoría para cualquier módulo nuevo

- Nunca registrar contraseñas, hashes, sales ni el contenido completo de una publicación.
- Acciones en MAYÚSCULAS y cortas. Errores inesperados con `Dialogos.errorInesperado(...)` o
  `RegistroAuditoria.describirError(ex)`.
