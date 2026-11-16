# Contratos compartidos: cómo integra Sebastián su módulo

Este documento explica qué piezas de la parte de Andrey puede usar Sebastián, y en qué dos puntos
de `Main.java` conecta su módulo (amistades, publicaciones y ventana principal real). **No hace
falta modificar ninguna otra clase de Andrey.**

> Convenciones del proyecto: Java 17, nombres en español (excepto `Main`), comentarios y Javadoc
> en español, archivos en UTF-8, dependencias inyectadas por constructor (nada de singletons).

---

## 1. Piezas que puede usar

| Contrato | Paquete | Para qué lo usa Sebastián |
|----------|---------|---------------------------|
| `DirectorioUsuarios` (interfaz, solo lectura) | `una.social.servicio` | Buscador de usuarios y mostrar nombres de amigos / autores: `buscarPorId("U001")`, `buscarPorNombreUsuario("ana")`, `listarTodos()` (lista no modificable). |
| `Usuario` (inmutable) | `una.social.modelo` | El usuario activo y los resultados de búsqueda. Use `getId()` para relacionar amistades y publicaciones. |
| `ContextoSesion` | `una.social.sesion` | `getUsuarioActivo()` para saber quién publica o agrega amigos. |
| `FabricaVentanaPrincipal` (interfaz) | `una.social.ui` | Crear su ventana principal real (ver punto 3). |
| `RegistroAuditoria` + `NivelAuditoria` | `una.social.auditoria` | Auditar sus eventos: `registrar(NivelAuditoria.INFO, actor, "AMISTAD_AGREGADA", "U001 -> U002")`. Nunca lanza excepciones. |
| `AlmacenArchivoTexto` + `LineaArchivo` | `una.social.persistencia` | Leer y escribir `amistades.txt` y `publicaciones.txt` con la misma convención (ver punto 4). |
| `ModuloPersistente` (interfaz) | `una.social.persistencia` | Implementarla en sus servicios para que se carguen al iniciar y se guarden al cerrar sesión y al cerrar la app. |
| Jerarquía de excepciones | `una.social.excepciones` | Lanzar o heredar de `ExcepcionSocialUna`, `ExcepcionValidacion`, `ExcepcionPersistencia`, `ExcepcionDatosCorruptos`... La UI ya sabe mostrarlas. |
| `TemaUi` y componentes | `una.social.ui.tema` | Que su ventana se vea igual que el login: `crearBotonPrimario`, `crearBotonSecundario`, `crearCampoTexto`, `crearEtiqueta`, `crearTitulo`, `crearSubtitulo`, `crearTarjeta`, `crearFondoCentrado`, `crearEtiquetaError`, colores (`ROJO_UNA`, `AZUL_UNA`, `FONDO`...) y fuentes. |
| `Dialogos` | `una.social.ui` | Alertas consistentes: `exito`, `advertencia`, `error`, `errorInesperado` (este último además audita). |

## 2. Registrar sus módulos de datos en `Main`

En `Main.main`, en el bloque marcado `>>> SEBASTIÁN` (paso 3), cree sus servicios con sus
dependencias y agréguelos a la lista `modulos`:

```java
GestorUsuarios gestorUsuarios = EnsambladorServicios.crearGestorUsuarios(CARPETA_DATOS, auditoria, reloj);
ServicioAmistades amistades = new ServicioAmistades(            // clase de Sebastián
        new RepositorioAmistadesArchivoTexto(CARPETA_DATOS.resolve("amistades.txt"), auditoria),
        gestorUsuarios,                                         // como DirectorioUsuarios
        auditoria);
ServicioPublicaciones publicaciones = new ServicioPublicaciones(/* ... */);
List<ModuloPersistente> modulos = List.of(gestorUsuarios, amistades, publicaciones);
```

El orden importa: **usuarios primero**, porque amistades y publicaciones validan IDs de usuario
contra el directorio. Con eso, `CoordinadorPersistencia` los carga al iniciar y los guarda al
cerrar sesión y al cerrar la aplicación; si uno falla, los demás igual se guardan.

> Nota: el `ContextoSesion` se crea en el paso 5 de `Main`. Si sus servicios lo necesitan, mueva
> la línea `ContextoSesion sesion = new ContextoSesion();` antes del paso 3.

## 3. Registrar su ventana principal en `Main`

En el paso 6 (`>>> SEBASTIÁN`), cambie **una sola línea**:

```java
// Antes (ventana provisional de Andrey):
FabricaVentanaPrincipal fabricaVentanaPrincipal = VentanaPrincipalTemporal::new;

// Después (ventana real de Sebastián):
FabricaVentanaPrincipal fabricaVentanaPrincipal =
        (usuario, alCerrarSesion) -> new VentanaPrincipal(usuario, alCerrarSesion, amistades, publicaciones, gestorUsuarios, auditoria);
```

Reglas del contrato `FabricaVentanaPrincipal.crear(usuario, alCerrarSesion)`:

- Devuelva la ventana **sin mostrarla**: el controlador la centra, la muestra y oculta el login.
- Su botón "Cerrar sesión" solo debe llamar a `alCerrarSesion.run()`. El controlador guarda todos
  los módulos en segundo plano, audita el `LOGOUT`, cierra la ventana y vuelve al login.
- No cambie la operación de cierre: el controlador la pone en `EXIT_ON_CLOSE`, y al salir el gancho
  de cierre de `Main` guarda los datos y audita `APP_CIERRE`.
- Haga las operaciones de archivo fuera del hilo de Swing (`SwingWorker`), como el login.
- `VentanaPrincipalTemporal` puede quedarse en el proyecto o borrarse; ya no se usará.

## 4. Convención de archivos en `data/`

- Texto plano UTF-8, un registro por línea, columnas separadas por `|`.
- Las líneas que empiezan con `#` son comentarios (escriba un encabezado que explique las columnas).
- Escape dentro de un campo: `\|`, `\\`, `\n`, `\r`. **No lo haga a mano**: use `AlmacenArchivoTexto`.
- Una línea dañada se **omite y se audita** (acción `LINEA_CORRUPTA_OMITIDA`), sin detener la carga.
- Guardado siempre con `AlmacenArchivoTexto.escribirLineasAtomicamente` (todo o nada).

Formato sugerido (Sebastián decide el definitivo y lo documenta aquí):

| Archivo | Columnas sugeridas |
|---------|--------------------|
| `amistades.txt` | `idUsuarioA\|idUsuarioB\|fechaISO` (una línea por par; amistad no dirigida) |
| `publicaciones.txt` | `idPublicacion\|idAutor\|fechaISO\|texto` (IDs `P0001`, `P0002`...; el texto puede traer `\|` y saltos de línea, el escape los maneja) |

Ejemplo de lectura y escritura con las utilidades compartidas:

```java
// Leer: ignora comentarios y líneas vacías; crea el archivo si no existe.
for (LineaArchivo linea : AlmacenArchivoTexto.leerLineasDeDatos(archivo)) {
    try {
        String[] c = AlmacenArchivoTexto.leerRegistro(linea.texto(), 3); // valida 3 columnas y desescapa
        // ... validar IDs con DirectorioUsuarios y construir el objeto ...
    } catch (ExcepcionDatosCorruptos ex) {
        auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "LINEA_CORRUPTA_OMITIDA",
                archivo.getFileName() + " línea " + linea.numero() + ": " + ex.getMessage());
    }
}

// Escribir: encabezado + registros escapados, de forma atómica.
List<String> lineas = new ArrayList<>(AlmacenArchivoTexto.comoComentarios("idUsuarioA|idUsuarioB|fechaISO"));
lineas.add(AlmacenArchivoTexto.unirCampos("U001", "U002", Instant.now().toString()));
AlmacenArchivoTexto.escribirLineasAtomicamente(archivo, lineas);
```

## 5. Convención de IDs

- Usuarios: `U` + al menos 3 dígitos (`U001`, `U002`, ..., `U1000`). Los asigna `GestorUsuarios`
  (máximo existente + 1); nunca se reutilizan. Patrón: `Usuario.PATRON_ID`.
- Usuarios de prueba fijos: **U001 Ana Mora (`ana`), U002 Luis Rojas (`luis`), U003 Maria Vargas
  (`maria`)**. Los datos de ejemplo de amistades y publicaciones deben referenciar estos IDs.
- Se recomienda el mismo estilo para publicaciones (`P0001`...).

## 6. Auditoría: qué no hacer

- Nunca registre contraseñas, hashes ni sales.
- Use acciones en MAYÚSCULAS y cortas (`PUBLICACION_CREADA`, `AMISTAD_AGREGADA`, `BUSQUEDA`).
- Para errores inesperados use `Dialogos.errorInesperado(...)` o `RegistroAuditoria.describirError(ex)`.
