# Contratos compartidos entre los módulos de Andrey y Sebastián

Este documento explica qué piezas comparten los dos módulos y cómo se integró el módulo social
(amistades, solicitudes, bloqueos, sugerencias, publicaciones, me gusta, comentarios, perfiles,
anuncios y ventana principal). Del módulo de usuarios de Andrey solo se **agregó** una interfaz
nueva y pequeña (`ServicioCuenta`) para editar el nombre y cambiar la contraseña; las interfaces que
ya existían no cambiaron.

> Convenciones del proyecto: Java 17, nombres en español (excepto `Main`), comentarios y Javadoc
> en español, archivos en UTF-8, dependencias inyectadas por constructor (nada de singletons).

---

## 1. Piezas compartidas

| Contrato | Paquete | Cómo lo usa el módulo social |
|----------|---------|------------------------------|
| `DirectorioUsuarios` (interfaz, solo lectura) | `una.social.servicio` | Todos los gestores sociales validan IDs y obtienen nombres con `buscarPorId` y `listarTodos`. No pueden modificar usuarios. |
| `ServicioCuenta` (**nueva**) | `una.social.servicio` | `cambiarNombre(Usuario, String)` y `cambiarContrasena(Usuario, char[] actual, char[] nueva, char[] confirmacion)`. La implementa `GestorUsuarios`; la usa la pestaña "Mi perfil". |
| `Usuario` (inmutable) | `una.social.modelo` | Usuario activo y autores; su `getId()` relaciona todos los archivos sociales. |
| `ContextoSesion` | `una.social.sesion` | Nuevo `actualizarUsuarioActivo(Usuario)`: tras cambiar el nombre, la sesión guarda el `Usuario` nuevo (mismo ID). |
| `FabricaVentanaPrincipal` (interfaz) | `una.social.ui` | `Main` registra la `VentanaPrincipal` real (ver punto 3). |
| `RegistroAuditoria` + `NivelAuditoria` | `una.social.auditoria` | Cada acción nueva se audita (ver punto 6). |
| `AlmacenArchivoTexto`, `ArchivoRegistros`, `LineaArchivo` | `una.social.persistencia` | Escape de campos, lectura que omite y audita líneas dañadas, escritura atómica con encabezado. `ArchivoRegistros.leer` acepta ahora **varias cantidades de columnas** (para leer el formato viejo de `publicaciones.txt`). |
| `ModuloPersistente` (interfaz) | `una.social.persistencia` | Todos los gestores sociales la implementan a través de `GestorPersistenteBase` (ver punto 2). |
| Jerarquía de excepciones | `una.social.excepciones` | Nueva `ExcepcionReglaNegocio` (regla del negocio no cumplida: mensaje amigable, se muestra como advertencia). `ExcepcionAmistadInvalida` y `ExcepcionPublicacionInvalida` heredan de ella. |
| `TemaUi` y componentes | `una.social.ui.tema` | Botones, campos, tarjetas y colores de toda la ventana principal. |
| `Dialogos`, `TareaEnSegundoPlano` | `una.social.ui` | Alertas consistentes y trabajo de archivos fuera del hilo de Swing. |

## 2. Contratos propios del módulo social

Patrón de cada módulo: **interfaz pequeña de servicio** + **Gestor** que la implementa +
**Repositorio** (interfaz + `...ArchivoTexto`) + **estructura en memoria**. Los gestores nuevos
(bloqueos, me gusta, comentarios, perfiles y anuncios) heredan de `GestorPersistenteBase` (método
plantilla): carga/guardado auditados, bandera `datosCargados` (si la carga falló, no se guarda
encima del archivo) y `ejecutarAuditado`, que audita la acción, su rechazo (`..._RECHAZADO`, WARN)
o su falla (`..._FALLIDO`, ERROR). `GestorAmistades` y `GestorPublicaciones` ya existían e
implementan `ModuloPersistente` directamente con la misma lógica (bandera incluida), para no
cambiar sus eventos de auditoría ni sus pruebas.

| Interfaz | Métodos | Implementación | La usa |
|----------|---------|----------------|--------|
| `ConsultaAmistades` | `idsAmigos`, `sonAmigos` | `GestorAmistades` | publicaciones, buscador, sugerencias |
| `ConsultaSolicitudes` | `haySolicitudPendiente` | `GestorAmistades` | sugerencias |
| `ServicioAmistades` (extiende las dos anteriores) | `enviarSolicitud` → `ResultadoEnvioSolicitud`, `aceptarSolicitud`, `rechazarSolicitud`, `cancelarSolicitud`, `eliminarAmistad`, `listarAmigos`, `listarSolicitudesRecibidas`, `listarSolicitudesEnviadas`, `agregarAmistad` (directo, para datos de ejemplo y pruebas) | `GestorAmistades` | UI |
| `ServicioBusqueda` | `buscar` | `BuscadorUsuarios` | `PanelBuscador` |
| `ServicioSugerencias` | `sugerir` (BFS, máx. 10), `amigosEnComun` | `SugeridorAmistades` | `PanelBuscador`, perfiles |
| `ConsultaBloqueos` | `hayBloqueoEntre` (en cualquier sentido) | `GestorBloqueos` | todos los módulos que muestran contenido |
| `ServicioBloqueos` (extiende `ConsultaBloqueos`) | `bloquear`, `desbloquear`, `bloqueoA`, `listarBloqueados` | `GestorBloqueos` | UI |
| `OyenteBloqueo` | `alBloquear` | `GestorAmistades` (rompe amistad y solicitudes) | `GestorBloqueos` lo notifica |
| `ConsultaPublicaciones` | `autorDe`, `puedeVer` | `GestorPublicaciones` | me gusta, comentarios |
| `ServicioPublicaciones` (extiende `ConsultaPublicaciones`) | `publicar(autor, texto, privacidad)`, `obtenerMuro` ("Mis amigos"), `obtenerMuroGeneral`, `obtenerPerfil`, `ultimaPublicacionDe`, `editarUltima`, `puedeDeshacer`, `deshacerUltima`, `borrar`, `compartir`, `quitarCompartido` | `GestorPublicaciones` | UI |
| `OyenteBorradoPublicacion` | `alBorrarPublicacion` | `GestorMeGusta`, `GestorComentarios` (borrado en cascada) | `GestorPublicaciones` lo notifica |
| `ServicioMeGusta` | `alternarMeGusta` → `EstadoMeGusta(cantidad, meGustaMio)`, `estado` | `GestorMeGusta` | tarjetas de publicación |
| `ServicioComentarios` | `comentar`, `borrar`, `listar` → `ComentarioVisible`, `contar` | `GestorComentarios` | `DialogoComentarios` |
| `ServicioPerfiles` | `obtenerBiografia`, `actualizarBiografia` | `GestorPerfiles` | perfiles |
| `ServicioAnuncios` | `publicar`, `borrar`, `listar` → `EntradaAnuncio` | `GestorAnuncios` | `PanelAnuncios` |

Los **oyentes** (patrón Observador) evitan dependencias circulares: `GestorBloqueos` no conoce a
`GestorAmistades` y `GestorPublicaciones` no conoce a me gusta ni a comentarios; solo avisan a
quien se haya registrado. Los registra `EnsambladorSocial`.

## 3. Integración en `Main`

1. **Servicios**: `EnsambladorSocial.crear(carpeta, directorio, auditoria, reloj)` arma todos los
   módulos en orden de dependencias, registra los oyentes y los devuelve juntos en el record
   `ModulosSociales`.
2. **Carga en tres fases** (cada fase valida referencias a la anterior):
   1. Usuarios (y los 3 de prueba si el catálogo está vacío).
   2. `fasePrincipal()`: bloqueos, amistades y solicitudes, publicaciones y compartidos, perfiles y
      anuncios (validan que los usuarios existan).
   3. `faseInteracciones()`: me gusta y comentarios (validan que las publicaciones existan).

   Si una fase falla, las que dependen de ella **no se cargan** y quedan protegidas contra un
   guardado que borraría sus archivos.
3. **Datos de ejemplo** (`HerramientaDatosSemillaSocial`): solo si existen U001–U003 y no hay ningún
   dato social.
4. **Guardado**: un `CoordinadorPersistencia` con usuarios + `social.todos()` guarda al cerrar
   sesión y en el gancho de cierre de la app (cada acción ya se guardó al momento).
5. **Ventana principal**: los servicios viajan juntos en el record `ServiciosSociales`, como
   interfaces:

```java
ServiciosSociales servicios = new ServiciosSociales(social.publicaciones(), social.amistades(),
        social.buscador(), social.sugeridor(), social.bloqueos(), social.meGusta(), social.comentarios(),
        social.perfiles(), social.anuncios(), gestorUsuarios, sesion, auditoria);
FabricaVentanaPrincipal fabricaVentanaPrincipal =
        (usuario, alCerrarSesion) -> new VentanaPrincipal(usuario, alCerrarSesion, servicios);
```

## 4. Formatos de archivo en `data/`

Convención común: UTF-8, un registro por línea, columnas separadas por `|`, líneas con `#` son
comentarios, escape `\|`, `\\`, `\n`, `\r`, escritura atómica. Cada archivo lleva un encabezado `#`
que explica sus columnas.

| Archivo | Columnas | Reglas al cargar |
|---------|----------|------------------|
| `usuarios.txt` | `id\|nombre\|usuario\|contrasena\|fechaISO` (sin cambios) | ID `U###`, contraseña no vacía y fecha válida, sin usuarios repetidos |
| `amistades.txt` | `idUsuarioA\|idUsuarioB\|fechaISO` | ambos IDs existen, distintos, par no repetido (en cualquier orden), sin bloqueo entre ellos |
| `solicitudes.txt` | `idRemitente\|idDestinatario\|fechaISO` | ambos existen, distintos, no son amigos, sin otra solicitud entre los dos, sin bloqueo; se cargan en orden de fecha (cola) |
| `bloqueos.txt` | `idBloqueador\|idBloqueado\|fechaISO` | ambos existen, distintos, no repetido |
| `publicaciones.txt` | `id\|idAutor\|fechaISO\|privacidad\|fechaEdicionISO\|texto` | ID `P####` no repetido, autor existe, privacidad `PUBLICA`/`SOLO_AMIGOS`, edición vacía o fecha válida, texto 1–500. **Compatibilidad**: también acepta `id\|idAutor\|fechaISO\|texto` (formato viejo → `SOLO_AMIGOS`, sin edición); al guardar se escribe el formato nuevo |
| `compartidos.txt` | `id\|idUsuario\|idPublicacion\|fechaISO` | ID `CP####`, usuario existe, no repetido por usuario y publicación. La publicación puede **no** existir (se muestra "no disponible") |
| `megusta.txt` | `idPublicacion\|idUsuario\|fechaISO` | publicación y usuario existen, sin repetidos |
| `comentarios.txt` | `id\|idPublicacion\|idAutor\|fechaISO\|texto` | ID `CM####` no repetido, publicación y autor existen, texto 1–300 |
| `perfiles.txt` | `idUsuario\|biografia` | usuario existe, no repetido, biografía 1–160 (vacía = no se guarda la línea) |
| `anuncios.txt` | `id\|idAutor\|fechaISO\|titulo\|texto` | ID `A####` no repetido, autor existe, título 1–80 en una línea, texto 1–500 |

Toda línea que no cumple se **omite y se audita** (`LINEA_CORRUPTA_OMITIDA`) y la carga continúa.

## 5. Convención de IDs

- Usuarios `U###`, publicaciones `P####`, compartidos `CP####`, comentarios `CM####`, anuncios `A####`.
- Se generan como "mayor número existente + 1". Para las publicaciones, "existente" incluye
  también los IDs a los que apuntan los compartidos (aunque la publicación ya se haya borrado) y los
  ya usados en esta ejecución: así un compartido "no disponible" nunca termina apuntando a otra
  publicación nueva (los me gusta y comentarios de la borrada ya se eliminaron en cascada).
- Usuarios de prueba fijos: **U001 Ana Mora (`ana`), U002 Luis Rojas (`luis`), U003 Maria Vargas
  (`maria`)**.

## 6. Reglas de auditoría

- Nunca registrar contraseñas, hashes, sales ni el texto completo de publicaciones, comentarios,
  biografías o anuncios: solo IDs, cantidades y largos.
- Acciones en MAYÚSCULAS y cortas. Las nuevas: `SOLICITUD_ENVIADA` (si se aceptó sola, el
  detalle lo indica), `SOLICITUD_ACEPTADA`, `SOLICITUD_RECHAZADA`, `SOLICITUD_CANCELADA`,
  `AMISTAD_ELIMINADA`, `USUARIO_BLOQUEADO`, `USUARIO_DESBLOQUEADO`, `RELACION_ROTA_POR_BLOQUEO`,
  `PUBLICACION_EDITADA`, `PUBLICACION_DESHECHA`, `PUBLICACION_BORRADA`, `PUBLICACION_COMPARTIDA`,
  `COMPARTIDO_QUITADO`, `ME_GUSTA`, `COMENTARIO_CREADO`, `COMENTARIO_BORRADO`,
  `ME_GUSTA_BORRADOS_EN_CASCADA`, `COMENTARIOS_BORRADOS_EN_CASCADA`, `BIOGRAFIA_ACTUALIZADA`,
  `ANUNCIO_PUBLICADO`, `ANUNCIO_BORRADO`, `NOMBRE_CAMBIADO`, `CONTRASENA_CAMBIADA` (y sus
  variantes `_RECHAZADO` / `_FALLIDO`).
- Errores inesperados con `Dialogos.errorInesperado(...)` o `RegistroAuditoria.describirError(ex)`.
