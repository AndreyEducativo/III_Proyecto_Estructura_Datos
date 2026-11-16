# Social UNA - Proyecto 3

Red social estudiantil de la Universidad Nacional (Costa Rica), hecha en **Java 17 + Swing**.
Los usuarios se registran, inician sesión, buscan estudiantes (con sugerencias de "Personas que
quizás conozcas"), se envían solicitudes de amistad, publican en un muro (público o solo para
amigos), dan "me gusta", comentan, comparten, editan su perfil, bloquean usuarios y publican
anuncios para toda la comunidad. Toda la información vive en memoria (estructuras de datos) y se
guarda en archivos de texto `.txt` dentro de `data/` (sin bases de datos).

<!-- Este README sirve a la vez de manual de ejecución y de bitácora de IA (al final). -->

## Pendientes del foro

El enunciado indica que las instrucciones del profesor en el foro del curso son parte del
enunciado. Anótelas aquí en cuanto se publiquen (fecha, instrucción y estado):

| Fecha | Instrucción del profesor | Estado |
|-------|--------------------------|--------|
| —     | (ninguna registrada todavía) | —  |

## Requisitos

- **Un solo JDK: Java 17 o superior** (el proyecto compila para Java 17; cualquier JDK 17, 21 o
  25 sirve). Es lo ÚNICO que hay que instalar.
- **No hace falta instalar Maven**: el proyecto trae el *Maven Wrapper* (`mvnw` / `mvnw.cmd`), que
  descarga Maven 3.9.9 solo la primera vez (requiere Internet en esa primera ejecución).
- Si en la máquina hay varios Java (por ejemplo un Java 8 viejo), defina `JAVA_HOME` apuntando al
  JDK 17+. Los scripts verifican la versión y avisan si es menor que 17.

## Cómo ejecutar

| Acción | Windows | Linux / macOS |
|--------|---------|---------------|
| Compilar y abrir la app | `run.bat` | `sh run.sh` |
| Ejecutar las pruebas | `mvnw.cmd test` | `./mvnw test` |
| Solo compilar y empaquetar (con pruebas) | `mvnw.cmd package` | `./mvnw package` |
| Abrir el JAR ya compilado | `java -ea -jar target\social-una.jar` | `java -ea -jar target/social-una.jar` |
| Regenerar los 3 usuarios de prueba | `java -cp target\social-una.jar una.social.herramientas.HerramientaDatosSemilla --reiniciar` | igual, con `/` |

Notas:
- La app siempre debe ejecutarse **desde la carpeta del proyecto** (los scripts ya lo hacen), porque
  lee y escribe en `data/`.
- `-ea` habilita los `assert` (invariantes internas). Las pruebas también corren con `-ea`
  (configurado en `pom.xml`).
- Si `data/` o algún archivo no existe, se crea solo. Si `usuarios.txt` está vacío, la app crea los
  3 usuarios de prueba; si además no hay ningún dato social, crea también los de ejemplo.
- **Volver a los datos de ejemplo**: cerrar la app, borrar de `data/` todos los `.txt` menos
  `usuarios.txt` y abrirla de nuevo.

## Usuarios y datos de prueba

| ID | Nombre | Usuario | Contraseña |
|----|--------|---------|------------|
| U001 | Ana Mora | `ana` | `ana12345` |
| U002 | Luis Rojas | `luis` | `luis12345` |
| U003 | Maria Vargas | `maria` | `maria12345` |

Datos sociales de ejemplo (creados por `HerramientaDatosSemillaSocial` con los mismos servicios que
usa la interfaz, así pasan por las mismas validaciones):

```
  Ana (U001) ── Luis (U002) ── Maria (U003)        Maria → Ana: solicitud pendiente
```

- 6 publicaciones (2 por usuario). **Públicas**: P0001 de Ana y P0003 de Maria; las demás son
  "Solo amigos". Hay "me gusta", 2 comentarios, **Luis compartió P0003** (de Maria), las 3
  biografías y 2 anuncios (de Maria y de Luis).

| Inicia sesión | "Mis amigos" | "Muro general" |
|---------------|--------------|----------------|
| `ana` | 5: sus 2 + las 2 de Luis + el compartido de Luis (no ve las "Solo amigos" de Maria) | 3: P0001, P0003 y el compartido |
| `luis` | 7: las 6 + su compartido | 3 |
| `maria` | 5: sus 2 + las 2 de Luis + el compartido de Luis | 3 |

## Funciones (resumen)

Descripción pantalla por pantalla en [docs/CONTEXTO_VIDEO.md](docs/CONTEXTO_VIDEO.md).

| Función | Dónde | Estructura de datos |
|---------|-------|---------------------|
| Solicitudes de amistad (enviar, aceptar, rechazar, cancelar; aceptación automática si ambos se envían) | Buscar, Solicitudes, perfiles | **Cola FIFO** por destinatario (`ArrayDeque`) |
| Personas que quizás conozcas (máx. 10, por amigos en común) | Buscar estudiantes | **BFS a 2 niveles** sobre el grafo + `HashMap` de amigos en común |
| Eliminar amigo | Mis amigos, perfiles | quita la arista del grafo |
| Privacidad Pública / Solo amigos (por defecto Solo amigos) | Muro | columna nueva en `publicaciones.txt` |
| Muro "Mis amigos" y "Muro general" | Muro | lista cronológica recorrida al revés + **mezcla** de listas ordenadas |
| Me gusta (alterna; solo se muestra la cantidad) | tarjetas | `HashMap<idPublicacion, Map<idUsuario, MeGusta>>` |
| Comentarios (máx. 300; del más antiguo al más reciente; borran su autor o el de la publicación) | "Ver comentarios (N)" | `HashMap<idPublicacion, List<Comentario>>` |
| Compartir (solo públicas de otros, una vez) | tarjetas | lista cronológica + `Set` de pares usuario-publicación |
| Editar la última publicación ("(editada)") y deshacer | tarjetas, Mi perfil | **Pila LIFO** por usuario (`ArrayDeque`) |
| Borrar cualquier publicación propia (con cascada) | tarjetas | patrón Observador |
| Biografía (máx. 160) | Mi perfil | `perfiles.txt` (sin tocar `usuarios.txt`) |
| Bloquear / desbloquear | perfiles, Mi perfil | `HashMap<bloqueador, Map<bloqueado, Bloqueo>>` |
| Editar nombre y cambiar contraseña | Mi perfil | `ServicioCuenta` (módulo de Andrey) |
| Anuncios públicos (título máx. 80, texto máx. 500) | Anuncios | lista cronológica + `HashMap` por ID |
| Perfil de otro usuario (amigos en común, estado, bloquear) | clic en cualquier nombre | — |

Atajos: Alt+M (Muro), Alt+N (Anuncios), Alt+B (Buscar), Alt+A (Amigos), Alt+S (Solicitudes),
Alt+P (Mi perfil). **Cerrar sesión** guarda todo y vuelve al login; la X también guarda.

### Demostración sugerida (paso a paso)

1. Entrar como `ana`: aparece el aviso "Maria Vargas te envió una solicitud" → **Decidir después**.
2. **Muro**: escribir algo, elegir **Pública** y **Publicar**. Cambiar a **Muro general**: se ven
   las públicas de todos. Pulsar **Me gusta** (cambia a "Te gusta (1)") y **Ver comentarios** para
   comentar.
3. **Mi perfil**: **Editar biografía**; **Editar última publicación** (aparece "(editada)");
   **Deshacer última publicación** (desaparece con sus me gusta y comentarios).
4. **Solicitudes**: **Rechazar** la de Maria. En **Buscar estudiantes** aparece Maria en
   "Personas que quizás conozcas" con **1 amigo en común** (Luis).
5. Pulsar el nombre de Maria → su perfil (solo sus públicas, porque no son amigas) → **Bloquear**.
   Maria desaparece del muro general, los anuncios y las sugerencias. Desbloquear en **Mi perfil →
   Usuarios bloqueados** (la amistad no vuelve sola).
6. **Anuncios**: publicar uno; entrar como `luis` y verlo. Como `luis`, en el muro, **Compartir**
   la publicación pública de Ana.
7. Como `maria`, en **Buscar estudiantes**, **Enviar solicitud** a Ana; entrar como `ana` y
   **Aceptar**: ahora se ven las "Solo amigos" de las dos.
8. **Mi perfil → Cambiar contraseña**; cerrar sesión y entrar con la nueva (la vieja ya no sirve).

## Archivos de datos (`data/`)

Todos son texto plano UTF-8, un registro por línea, columnas separadas por `|`. Las líneas que
empiezan con `#` son comentarios (cada archivo explica sus columnas en su encabezado). Dentro de
un campo se escapan: `\|` (barra vertical), `\\` (barra invertida), `\n` y `\r` (saltos de línea).

| Archivo | Formato | Dueño |
|---------|---------|-------|
| `usuarios.txt` | `id\|nombre\|usuario\|contrasena\|fechaISO` (contraseña en texto plano a propósito, para la defensa; **formato sin cambios**) | Andrey |
| `auditoria.log` | `fechaISO\|nivel\|actor\|accion\|detalle\|hashAnterior\|hashActual` (lo genera la app; no se versiona) | Andrey |
| `amistades.txt` | `idUsuarioA\|idUsuarioB\|fechaISO` (una línea por amistad; la amistad es mutua) | Sebastián |
| `solicitudes.txt` | `idRemitente\|idDestinatario\|fechaISO` (solo PENDIENTES; al aceptarla pasa a `amistades.txt`) | Sebastián |
| `bloqueos.txt` | `idBloqueador\|idBloqueado\|fechaISO` | Sebastián |
| `publicaciones.txt` | `idPublicacion\|idAutor\|fechaISO\|privacidad\|fechaEdicionISO\|texto` (privacidad `PUBLICA` o `SOLO_AMIGOS`; edición vacía si nunca se editó). **Acepta también el formato viejo** `id\|autor\|fecha\|texto` (se lee como Solo amigos y se reescribe en el nuevo al guardar) | Sebastián |
| `compartidos.txt` | `idCompartido\|idUsuario\|idPublicacion\|fechaISO` (IDs `CP0001`...) | Sebastián |
| `megusta.txt` | `idPublicacion\|idUsuario\|fechaISO` | Sebastián |
| `comentarios.txt` | `idComentario\|idPublicacion\|idAutor\|fechaISO\|texto` (IDs `CM0001`...) | Sebastián |
| `perfiles.txt` | `idUsuario\|biografia` | Sebastián |
| `anuncios.txt` | `idAnuncio\|idAutor\|fechaISO\|titulo\|texto` (IDs `A0001`...) | Sebastián |

Una línea dañada de un archivo de datos **no tumba la app**: se omite, se registra en la auditoría
(`LINEA_CORRUPTA_OMITIDA`) y se cargan las demás. Lo mismo pasa con referencias imposibles (un
usuario o publicación que no existe, IDs repetidos, un usuario amigo de sí mismo...).

## Estructuras de datos en memoria

| Qué | Estructura | Por qué |
|-----|-----------|---------|
| Catálogo de usuarios | `ArrayList<Usuario>` + `HashMap` por usuario y por ID | lista dinámica en orden de registro; búsquedas O(1) |
| Amistades | **Grafo no dirigido con listas de adyacencia**: `Map<String, LinkedHashSet<String>>` + `List<Amistad>` (aristas) | "¿quiénes son mis amigos?" y "¿somos amigos?" en O(1) |
| Solicitudes recibidas | **Cola** (`ArrayDeque`) por destinatario | se atienden en el orden en que llegaron (FIFO) |
| Sugerencias | **BFS** con `ArrayDeque` + `HashMap` de distancias y de amigos en común | amigos de mis amigos (nivel 2), contando caminos |
| Publicaciones | `ArrayList<Publicacion>` cronológica + `HashMap` por ID | el muro se arma recorriendo la lista de atrás hacia adelante, sin ordenar |
| Deshacer | **Pila** (`ArrayDeque`) de IDs por usuario | se deshace primero lo último publicado (LIFO) |
| Compartidos | `ArrayList` cronológica + `HashMap` por ID + `Set` de pares | se mezclan con las publicaciones (mezcla de listas ordenadas) |
| Me gusta | `HashMap<idPublicacion, Map<idUsuario, MeGusta>>` | alternar y contar en O(1) |
| Comentarios | `HashMap<idPublicacion, List<Comentario>>` + `HashMap` por ID | lista en orden de llegada por publicación |
| Bloqueos | `HashMap<bloqueador, Map<bloqueado, Bloqueo>>` | "¿hay bloqueo entre A y B?" en O(1), en los dos sentidos |
| Biografías | `HashMap<idUsuario, String>` | una por usuario |
| Anuncios | `ArrayList` cronológica + `HashMap` por ID | del más reciente al más antiguo recorriendo al revés |

Los algoritmos (BFS, cola, pila, mezcla, cascada) están explicados paso a paso en
[docs/UML.md](docs/UML.md#algoritmos-explicados-para-la-defensa).

## Estructura del código

```
src/main/java/una/social/
├── Main.java                     arranque y "raíz de composición" (único nombre en inglés)
├── EnsambladorServicios.java     crea y conecta el módulo de usuarios        (Andrey)
├── EnsambladorSocial.java        crea y conecta todo el módulo social        (Sebastián)
├── ModulosSociales.java          los módulos sociales juntos y sus fases de carga
├── amistades/      grafo, cola de solicitudes, gestor, buscador, sugerencias (BFS)   (Sebastián)
├── bloqueos/       bloqueos entre usuarios y su oyente                               (Sebastián)
├── publicaciones/  publicación, privacidad, historial, compartidos, muro, pila       (Sebastián)
├── interacciones/  me gusta y comentarios                                            (Sebastián)
├── perfiles/       biografía                                                         (Sebastián)
├── anuncios/       tablón de anuncios públicos                                       (Sebastián)
├── auditoria/      bitácora append-only con hash SHA-256 encadenado y su verificador
├── excepciones/    jerarquía ExcepcionSocialUna y subclases
├── herramientas/   datos de prueba: usuarios y datos sociales de ejemplo
├── modelo/         Usuario (inmutable), CatalogoUsuarios (ArrayList + HashMap), CampoFormulario
├── persistencia/   AlmacenArchivoTexto, ArchivoRegistros y GestorPersistenteBase (compartidos)
├── seguridad/      hasheadores de contraseña, control de intentos fallidos
├── servicio/       interfaces pequeñas (DirectorioUsuarios, ServicioAutenticacion,
│                   ServicioRegistro, ServicioCuenta) y GestorUsuarios
├── sesion/         ContextoSesion (usuario activo)
└── ui/             login, registro, VentanaPrincipal y sus pestañas, perfiles y diálogos
    └── tema/       TemaUi y componentes con estilo (compartido)
src/test/java/...   106 pruebas JUnit 5 + oyente que audita las ejecuciones (nivel TEST)
docs/               CONTRATOS.md (contratos entre módulos), UML.md (diagramas Mermaid y
                    algoritmos) y CONTEXTO_VIDEO.md (pantallas y guion del video)
```

## Decisiones tomadas (y por qué)

1. **Una sola versión de Java (17) y Maven Wrapper**: solo hay que instalar un JDK; no hace falta
   instalar Maven ni extensiones extra.
2. **Nombres en español** en clases, métodos y archivos (excepto `Main`), para ser consistentes
   con los comentarios. Los paquetes también están en español (`persistencia`, `ui.tema`...).
3. **Reglas de validación** (el enunciado pide "campos vacíos"; se eligió lo más simple y seguro):
   nombre ≤ 60 caracteres; usuario de 3 a 20 caracteres con letras sin tilde, números, `.`, `-`
   o `_`; contraseña de 6 a 128 caracteres, no solo espacios; confirmación igual. Cambiar el nombre
   usa las mismas reglas; el ID y el usuario no cambian nunca.
4. **Bloqueo de inicio de sesión**: 5 intentos fallidos → 5 minutos, en memoria (se olvida al
   reiniciar).
5. **Contraseñas en texto plano en `usuarios.txt`** (decisión de Andrey, para la defensa) y **se
   respetó**: "Cambiar contraseña" pasa por la interfaz `HasheadorContrasenas`, así que con
   `HasheadorPbkdf2` (PBKDF2WithHmacSHA256, 310 000 iteraciones, sal de 16 bytes) la contraseña
   nueva se protege con una **sal nueva** (hay una prueba que lo comprueba). Las contraseñas viajan
   como `char[]` y se borran con `Arrays.fill`.
6. **El log de pruebas** se escribe aparte, en `target/auditoria-pruebas.log`, con nivel TEST.
7. **Limitación conocida de la auditoría**: borrar las ÚLTIMAS líneas del log no se puede detectar
   solo con la cadena de hashes. Editar, borrar o insertar líneas intermedias sí se detecta.
8. **Amistad por solicitud**: A envía, B acepta o rechaza, A puede cancelar. Si B ya le había
   enviado a A, al enviar A se **aceptan automáticamente** (no tiene sentido dejar dos solicitudes
   cruzadas). Las solicitudes no se permiten si ya son amigos, si ya hay una pendiente o si hay un
   bloqueo. El "Agregar como amigo" directo ya no está en la interfaz (solo se usa para crear los
   datos de ejemplo y en pruebas).
9. **Publicaciones**: máximo 500 caracteres, **por defecto "Solo amigos"** (lo más seguro). La
   privacidad no se puede cambiar después de publicar; editar solo cambia el texto y solo la
   publicación **más reciente** del autor. Borrar se puede con cualquiera propia.
10. **Deshacer** usa una pila en memoria con lo publicado **desde que se abrió la app** (no se
    guarda en disco): deshacer algo de días atrás sería sorprendente; para eso está "Borrar". Si se
    borra a mano una publicación que estaba en la pila, "Deshacer" la salta.
11. **El ID de una publicación borrada no se reutiliza mientras algo lo referencie**: el siguiente
    ID es el mayor entre las publicaciones actuales, los IDs a los que apuntan los compartidos y los
    ya usados en esta ejecución. Así un compartido "no disponible" jamás termina mostrando otra
    publicación nueva con el mismo ID (los me gusta y comentarios se borran en cascada).
12. **Compartir**: solo publicaciones públicas de otras personas y una sola vez cada una. Si la
    original se borra, el compartido queda con "La publicación original ya no está disponible" (no
    se borra en cascada, para no quitarle a alguien algo que hizo). Quien compartió puede quitarlo.
13. **Me gusta y comentarios** solo en publicaciones que el usuario puede ver. Se permite dar "me
    gusta" a la propia publicación. Nunca se muestra quién dio "me gusta", solo la cantidad.
14. **Bloquear** borra la amistad y las solicitudes entre ambos y oculta en los dos sentidos:
    publicaciones (también los compartidos de sus publicaciones), comentarios, anuncios, búsqueda,
    sugerencias y perfil. **Desbloquear no devuelve la amistad**: hay que enviar otra solicitud.
15. **Anuncios**: cualquier usuario registrado puede publicar (el enunciado no habla de roles);
    solo el autor puede borrarlos. El título es de una sola línea.
16. **Biografía** en `perfiles.txt` para no cambiar el formato de `usuarios.txt` (que es de
    Andrey). Dejarla vacía la elimina del archivo.
17. **Guardar en cada cambio**: cada acción se guarda en disco **antes** de cambiar la memoria (si
    falla el disco, la memoria no cambia), además de al cerrar sesión y al cerrar la app.
18. **Carga en tres fases**: usuarios → (bloqueos, amistades, publicaciones, perfiles, anuncios) →
    (me gusta, comentarios), porque cada fase valida que existan los IDs de la anterior. Si una
    fase falla, las siguientes no se cargan ni se guardan, para no perder sus archivos.

## Bitácora de IA

### Entrada 1
- Fecha y hora: 2026-11-15 17:19:02 CST
- Autor: Andrey
- Herramienta: Codex en VS Code
- Propósito: Diseñar y construir la parte de Andrey del proyecto Social UNA según el PDF entregado y la rúbrica.

```text
ROL
Eres un ingeniero de software senior en Java. Vas a ayudarme a construir mi parte de un proyecto universitario. Se adjunta el PDF "Proyecto_3.pdf" (enunciado y rúbrica). Léelo completo antes de escribir código. Todo lo que dice el PDF es obligatorio y tiene prioridad sobre cualquier suposición tuya. Además, el PDF indica que el profesor dará instrucciones adicionales en un foro del curso que cuentan como parte del enunciado: deja en el README una sección "Pendientes del foro" para anotarlas.

IMPORTANTE: SOLO TRABAJAS EN LA PARTE DE ANDREY
El proyecto lo hacen dos personas: Andrey (yo, quien te escribe) y Sebastián. Tú solo debes implementar la PARTE DE ANDREY (sección 4). NO implementes la parte de Sebastián (sección 5): no escribas sus clases, ventanas ni lógica. Lo único que puedes crear de su zona son los contratos compartidos (interfaces) descritos en la sección 6, y un placeholder mínimo reemplazable. Si dudas si algo es mío o de Sebastián, asume que NO es mío y déjalo como interfaz o TODO documentado.

1. PROYECTO
Red social estudiantil "Social UNA" (Universidad Nacional, Costa Rica). Los usuarios se registran, inician sesión, buscan estudiantes, los agregan como amigos y publican mensajes en un muro cronológico (de la más reciente a la más antigua) con sus publicaciones y las de sus amigos.

2. REQUERIMIENTOS OBLIGATORIOS DEL PDF
- Lenguaje: Java (17 o superior). Interfaz gráfica con Java Swing. Usa solo el JDK en tiempo de ejecución, sin librerías externas de UI; JUnit 5 solo para pruebas. Usa Maven (pom.xml) para compilar, probar y empaquetar un JAR ejecutable.
- No se evalúan proyectos por consola: toda interacción es con ventanas, botones, cuadros de texto y alertas gráficas (JOptionPane o diálogos propios).
- Toda la información en ejecución vive en memoria principal usando estructuras de datos: listas/vectores dinámicos para el catálogo de usuarios y el historial de publicaciones, y grafo o lista de adyacencia para las amistades (esto último es de Sebastián).
- Persistencia SOLO con archivos de texto (.txt). PROHIBIDO usar bases de datos (MySQL, SQL Server, SQLite, H2, etc.). No uses JSON ni librerías de serialización: formato propio de texto plano delimitado por "|" con escape de caracteres especiales (el delimitador, saltos de línea y la barra invertida). Al abrir la app se leen los archivos, se reconstruyen las estructuras en memoria y se reanuda donde se quedó. Guarda en cada cambio importante y también al cerrar sesión y al cerrar la aplicación.
- Registro: ID, Nombre, Usuario, Contraseña. Login validando credenciales leídas desde los archivos.
- Validación de campos vacíos en todos los formularios, sin bloqueos del sistema y con navegación fluida entre ventanas.
- Entregables: código fuente, instrucciones exactas para ejecutar (README), archivos de datos de ejemplo con al menos 3 usuarios pre-registrados, y un diagrama de clases UML básico en formato texto (Mermaid o PlantUML) dentro de la carpeta docs/ (opcional pero lo quiero).
- Rúbrica (úsala como criterio de calidad): GUI 20%, Estructuras de datos 30%, Persistencia 30%, Lógica de negocio 20%.

3. REQUERIMIENTOS DE CALIDAD (TRANSVERSALES A TODO TU CÓDIGO)
a) Principios SOLID, aplicados de forma real y explícita:
   - S: una responsabilidad por clase (modelo, repositorio, servicio, UI y auditoría separados).
   - O: extensible mediante interfaces y no modificando clases existentes.
   - L: las implementaciones deben poder sustituir a sus interfaces sin romper el comportamiento.
   - I: interfaces pequeñas y específicas.
   - D: la UI y los servicios dependen de abstracciones; inyecta las dependencias por constructor, sin singletons ocultos ni "new" de implementaciones dentro de la lógica.
   Agrega comentarios Javadoc breves que indiquen qué principio aplica cada clase importante.
b) Excepciones: jerarquía propia (por ejemplo SocialUnaException como base, con AuthenticationException, ValidationException, DuplicateUserException, PersistenceException, DataCorruptionException). Ningún error debe tumbar la aplicación: captura en los límites (UI y servicios), muestra mensajes amigables al usuario y registra el detalle técnico en la auditoría. Instala un Thread.setDefaultUncaughtExceptionHandler y el manejo equivalente para el hilo de Swing que muestre un diálogo y audite el error. Si una línea de un archivo de datos está corrupta, sáltala, audítala y continúa, sin crashear. Nunca uses catch vacíos.
c) Asserts: usa "assert" de Java para invariantes internas del programador (precondiciones de métodos privados, estados que nunca deberían ocurrir). Para validar entrada del usuario o de archivos usa excepciones, NO asserts (los asserts pueden estar desactivados). Configura Maven (surefire) y el script de ejecución para correr con -ea (assertions habilitadas) en desarrollo y pruebas.
d) Código limpio: nombres descriptivos en español o inglés (elige uno y sé consistente), métodos cortos, sin números mágicos, sin código duplicado, codificación UTF-8 en todos los archivos.
e) Pruebas unitarias JUnit 5 para tu parte: registro, login correcto e incorrecto, usuario duplicado, validaciones, persistencia (guardar y recargar), archivo corrupto, auditoría (append-only y verificación de integridad).

4. PARTE DE ANDREY (LO ÚNICO QUE DEBES IMPLEMENTAR)
4.1 Estructura y arranque del proyecto
- pom.xml, estructura de paquetes (base: una.social) y una clase Main que: inicializa auditoría y persistencia, carga datos, muestra la ventana de Login, y al autenticar abre la ventana principal mediante la interfaz MainWindowFactory (ver sección 6).
- Scripts de ejecución (run.bat y run.sh) y README con instrucciones exactas de compilar, ejecutar y probar.
4.2 Modelo y catálogo de usuarios
- Clase User: id, nombre, usuario, hash de contraseña, salt, fecha de registro.
- Catálogo de usuarios en memoria en una lista dinámica (ArrayList o una lista propia) más un índice auxiliar (por ejemplo HashMap usuario->User) para validar duplicados. Generación automática de IDs únicos con formato U001, U002, etc. (el usuario no tiene que escribir el ID, pero se muestra al registrarse).
- Servicio UserService (con interfaz) para registrar, buscar por id/usuario y listar.
4.3 Autenticación y seguridad
- Ventanas LoginFrame y RegisterFrame con navegación fluida entre ambas.
- Ocultamiento de contraseñas: usa JPasswordField (se muestra con puntos), con un checkbox opcional "Mostrar contraseña". Maneja la contraseña como char[] y bórrala de memoria (Arrays.fill) después de usarla.
- La contraseña NUNCA se guarda en texto plano: usa PBKDF2WithHmacSHA256 (javax.crypto) con salt aleatorio por usuario (SecureRandom) y un número alto de iteraciones; la comparación de hashes debe ser en tiempo constante (MessageDigest.isEqual). Guarda en el archivo salt y hash codificados en Base64.
- Reglas de validación: campos no vacíos, usuario único (sin distinguir mayúsculas), longitud mínima de contraseña (por ejemplo 6) y confirmación de contraseña en el registro. Límite de intentos fallidos de login (por ejemplo 5) con bloqueo temporal en memoria, auditando los intentos. Mensajes de error genéricos en el login ("Usuario o contraseña incorrectos") para no revelar si el usuario existe.
- SessionContext (o similar) con el usuario activo, y la acción de cerrar sesión que guarda los datos y regresa al login.
4.4 Persistencia de usuarios y utilidades compartidas de archivos
- Carpeta data/ con usuarios.txt (formato: id|nombre|usuario|saltBase64|hashBase64|fechaISO). Con un encabezado comentado opcional.
- Interfaz UserRepository con implementación TextFileUserRepository. Escritura atómica (escribir a un archivo temporal y luego moverlo con ATOMIC_MOVE / REPLACE_EXISTING) para no perder datos si la app se cierra a mitad de escritura. Si data/ o los archivos no existen, créalos automáticamente.
- Clase utilitaria compartida TextFileStore (en el paquete una.social.persistence) para que Sebastián la reutilice: leer todas las líneas, escribir líneas de forma atómica, escapar y desescapar campos, y dividir líneas respetando el escape.
- Herramienta SeedDataTool (clase con main) que genera data/usuarios.txt con 3 usuarios de prueba: U001 Ana Mora (usuario: ana), U002 Luis Rojas (usuario: luis), U003 Maria Vargas (usuario: maria), con contraseñas de prueba documentadas en el README. Documenta esos IDs en el README, porque Sebastián creará amistades.txt y publicaciones.txt de ejemplo que los referencian.
4.5 Log de auditoría (APPEND-ONLY, INMODIFICABLE)
- Interfaz AuditLogger con un único método de escritura, por ejemplo log(AuditLevel nivel, String actor, String accion, String detalle), con niveles INFO, WARN, ERROR y TEST. NO debe existir ningún método para editar, borrar, truncar ni reescribir entradas.
- Implementación FileAuditLogger sobre data/auditoria.log: abre SIEMPRE en modo append (StandardOpenOption.CREATE, APPEND), una línea por evento con: timestamp ISO-8601, nivel, usuario actor (o "SISTEMA"), acción, detalle, y un hash SHA-256 encadenado (cada línea incluye el hash de la línea anterior), para que cualquier edición o borrado manual sea detectable.
- Método de verificación AuditIntegrityVerifier que recorre el archivo y reporta si la cadena es íntegra o en qué línea se rompe (se ejecuta al iniciar la app y deja el resultado en el propio log; si está rota, muestra advertencia gráfica pero no bloquea la app).
- Debe ser seguro entre hilos (synchronized o lock) y NUNCA debe lanzar excepciones hacia afuera: si falla escribir el log, lo reporta por System.err como último recurso y continúa.
- Nunca registres contraseñas ni hashes en el log.
- Audita como mínimo: inicio y cierre de la app, carga y guardado de datos, registros, logins exitosos y fallidos, bloqueos, logouts, errores y excepciones (con tipo y mensaje, sin exponer datos sensibles), líneas corruptas omitidas, y las ejecuciones de pruebas (nivel TEST, por ejemplo un listener de JUnit o un helper que las registre en un log de pruebas aparte o con nivel TEST, para saber cuándo y cuántas pruebas se corrieron).
4.6 Tema visual compartido (interfaz bonita, intuitiva y fácil de entender)
- Paquete una.social.ui.theme con una clase UiTheme: paleta de colores moderna y coherente (por ejemplo azul/rojo institucional UNA con fondos claros), tipografías, márgenes, y métodos de fábrica para crear botones primarios y secundarios, campos de texto y etiquetas con estilo consistente (bordes redondeados, hover, foco visible). Sebastián la reutilizará.
- Las ventanas de Login y Registro deben verse profesionales: título "Social UNA", logo o texto de marca, formularios centrados, etiquetas claras, mensajes de error en línea en rojo, placeholders, tecla Enter para enviar, tabulación lógica, tamaño mínimo y diseño que se adapte al redimensionar. Usa alertas gráficas amigables para éxitos y errores.
- Toda operación pesada de archivos fuera del hilo de Swing si hay riesgo de congelar la UI (SwingWorker), y nunca bloquear la interfaz.

5. PARTE DE SEBASTIÁN (NO IMPLEMENTAR; SOLO PARA TU CONTEXTO)
- Módulo social: buscador de usuarios, solicitud o acción "Agregar como amigo", lista de amigos del usuario activo, modelado de amistades con grafo o lista de adyacencia, y persistencia en data/amistades.txt.
- Módulo de publicaciones: modelo Post, escribir y publicar mensajes, muro/feed cronológico (más reciente primero) con las publicaciones propias y de los amigos, persistencia en data/publicaciones.txt, y datos de ejemplo.
- La ventana principal real (MainFrame) con navegación entre muro, buscador y amigos.

6. CONTRATOS COMPARTIDOS (ESTO SÍ LO CREAS TÚ, PARA QUE CADA UNO TRABAJE POR SEPARADO)
Crea estas piezas con Javadoc claro, para que Sebastián las use sin tocar tu código:
- AuditLogger (interfaz) y FileAuditLogger.
- TextFileStore (utilidades de archivos).
- UiTheme (tema visual).
- UserDirectory (interfaz de solo lectura): findById(String), findByUsername(String), findAll() que devuelve una lista no modificable. Tu UserService la implementa.
- SessionContext o un objeto User inmutable de solo lectura para pasar el usuario activo.
- Jerarquía de excepciones base (SocialUnaException y subclases) para que Sebastián herede de ella.
- MainWindowFactory (interfaz) con un método tipo: JFrame create(User usuarioActivo, Runnable onLogout). Crea además PlaceholderMainFrame, una ventana mínima y bonita que muestra "Bienvenido, <nombre>" y un botón de cerrar sesión, registrada como implementación por defecto. Sebastián solo reemplazará esta implementación con su MainFrame real.
- Documenta en docs/CONTRATOS.md exactamente cómo integra Sebastián su módulo (qué interfaces usar, dónde registrar su MainWindowFactory en Main, convención de archivos en data/ y de IDs).

7. BITÁCORA DE IA (HAZLA DE UNA VEZ, ANTES DE PROGRAMAR)
El PDF no dice nada sobre la bitácora de IA, así que se lleva en el archivo README.md de la raíz del proyecto (ya existe; NO lo sobrescribas, agrega al final una sección "## Bitácora de IA"). Primero, antes de escribir código, crea esa sección con la primera entrada, que debe incluir:
- Fecha y hora, autor (Andrey), herramienta (Codex en VS Code) y propósito.
- El texto COMPLETO y literal de este prompt, en un bloque de código (el que acabo de enviarte).
- Un resumen de lo que respondiste y generaste, los archivos creados o modificados, y un apartado "Revisión humana" vacío con casillas para que Andrey anote qué revisó, corrigió o rechazó.
Cada vez que termines una etapa de trabajo, agrega una nueva entrada a la bitácora con el mismo formato (sin borrar las anteriores). La bitácora es solo para entradas de Andrey; Sebastián llevará la suya.

8. FORMA DE TRABAJAR
- Antes de programar, muéstrame brevemente tu plan (estructura de paquetes y lista de clases) y luego impleméntalo completo sin dejar fragmentos de "aquí va tu código".
- Entrega código compilable y ejecutable; compílalo y corre las pruebas para verificarlo, y corrige lo que falle.
- Si algo del PDF es ambiguo, elige la opción más simple y segura, documéntala en el README y avísame, sin inventar requisitos que contradigan el PDF.
- Al terminar, dame un resumen con: qué quedó listo, cómo ejecutarlo, qué le falta a Sebastián, y cualquier decisión que debo defender en la defensa del proyecto.

9) Comentarios en el código (OBLIGATORIO Y MUY IMPORTANTE):
   Yo debo poder entender, explicar y defender todo el código en la defensa del curso, así que comenta TODO, sin excepciones y en español:
   - Cada archivo .java empieza con un comentario de cabecera que indique: qué hace la clase o interfaz, su rol dentro de la arquitectura, qué principio SOLID aplica y con qué otras clases colabora.
   - Cada clase, interfaz, enum, constructor, método (públicos y privados) y atributo lleva Javadoc (/** ... */) con la descripción, @param, @return y @throws cuando corresponda. Explica también las precondiciones y poscondiciones importantes.
   - Dentro de los métodos añade comentarios línea a línea o por bloque que expliquen el PORQUÉ de cada decisión, no solo el qué. Por ejemplo: por qué se usa PBKDF2 y un salt, por qué la escritura es atómica, por qué el hash de la auditoría se encadena, por qué se usa assert aquí y excepción allá, por qué se usa SwingWorker.
   - Comenta cada bloque try/catch/finally indicando qué error se espera, cómo se maneja y qué ve el usuario.
   - Comenta cada assert indicando qué invariante protege.
   - Comenta de forma especial las partes más difíciles (criptografía, escritura atómica, escape de campos en los archivos .txt, cadena de hashes de la auditoría, validación de integridad, manejo de hilos de Swing). Explícalas con un lenguaje sencillo, como si se las explicaras a un estudiante que las ve por primera vez.
   - Comenta también los archivos que no son Java: pom.xml, run.bat, run.sh, README, y los archivos .txt de datos de ejemplo (con líneas de encabezado que comienzan con # y explican el formato de cada columna; el lector de archivos debe ignorar esas líneas).
   - Los comentarios deben ser claros y útiles, sin relleno ni comentarios obvios sin valor (por ejemplo "// suma 1" sobre i++), y siempre deben estar actualizados con el código. Si modificas código, actualiza su comentario.
   - Mantén un lenguaje consistente: todos los comentarios y Javadoc en español, con tildes correctas, en UTF-8.
```

### Resumen de la respuesta generada
Se creó el plan inicial del proyecto y se identificó que la parte de Andrey debe incluir los contratos compartidos, la persistencia con archivos de texto, la seguridad con PBKDF2, la auditoría encadenada, la UI de login/registro y el placeholder de ventana principal.

Se preparará la base de Maven, la lógica de usuarios, la auditoría, los modelos y la UI mínima funcional para que Sebastián pueda sustituir la ventana principal y continuar con la parte de amistades y publicaciones.

### Revisión humana
- [ ] Andrey revisó la estructura propuesta.
- [ ] Andrey corrigió o ajustó el plan.
- [ ] Andrey rechazó algún punto del diseño.

### Entrada 2
- Fecha y hora: 2026-11-15 18:51 (hora de Costa Rica)
- Autor: Sebastian
- Herramienta: Claude Code (extensión de VS Code)
- Propósito: Revisar el avance de la parte de Andrey contra el prompt de la Entrada 1 y el PDF, y
  luego corregir y completar esa parte "de manera super correcta", con una sola versión de Java y
  con los nombres de archivos en español (excepto `Main`).

Prompts de esta etapa (literales, en orden):

```text
Mira eso dime que hemos avanzdo y que falta por hacer segun ese prompt que tenemos. Ademas revisa en el archivo que te mande lo que hay por hacer y fijate que no tenga notas para la IA
```

```text
Arregla la parte de Andrey de lo que esta hecho, hazla de manera super correcta y ademas haz el trabajo para solo una version de Java (para no instalar tantas extensiones), y los archivos ponlos en nombre español menos el main
luego me dices que fue lo que corregiste y me lo explicas
```

### Resumen de la respuesta generada
- **Revisión inicial**: el PDF no contenía notas dirigidas a una IA. El código anterior NO compilaba
  (multi-catch inválido en el repositorio), todos los `.java` estaban sueltos en la raíz (Maven no
  podía compilarlos), una prueba fallaba, la auditoría no se usaba y su cadena de hashes se
  reiniciaba en cada arranque y no detectaba borrados.
- **Reestructuración**: estructura Maven estándar (`src/main/java`, `src/test/java`, `docs/`,
  `data/`), Java 17 fijo en el `pom.xml` y Maven Wrapper (solo hace falta un JDK). Se borraron los
  archivos temporales `src` y `zzzzdir`.
- **Renombrado al español** de clases, paquetes, métodos y archivos (excepto `Main`).
- **Reescritura y corrección** de toda la parte de Andrey: modelo inmutable, catálogo con ArrayList +
  HashMap, repositorio con validación por columna y omisión auditada de líneas corruptas, escritura
  atómica con `force` y reintentos, PBKDF2 con `char[]` de punta a punta, bloqueo por intentos con
  reloj inyectable, interfaces pequeñas (ISP), auditoría append-only con hash encadenado que continúa
  entre ejecuciones, verificador que detecta edición/borrado/inserción, manejador global de errores
  (incluido el hilo de Swing), SwingWorker para todo trabajo pesado, tema visual con botones
  redondeados, hover, foco visible, placeholders y errores en línea en rojo.
- **Pruebas**: 38 pruebas JUnit 5 (todas pasan) y un oyente que audita las ejecuciones con nivel TEST.
- **Verificación**: compilado y probado con `mvnw.cmd package`; app arrancada; flujo login → ventana
  principal → cerrar sesión probado de forma automática; ventanas revisadas en capturas.

Archivos creados o modificados: `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`,
`.gitignore`, `.gitattributes`, `run.bat`, `run.sh`, `README.md`, `docs/CONTRATOS.md`, `docs/UML.md`,
`data/usuarios.txt`, todo `src/main/java/una/social/**` y todo `src/test/**`. Eliminados: los 27
archivos `.java` sueltos de la raíz, `CONTRATOS.md`, `UML.md`, `src` y `zzzzdir` de la raíz.

### Revisión humana
- [ ] Revisé la nueva estructura de paquetes y los nombres en español.
- [ ] Ejecuté `run.bat` / `run.sh` e inicié sesión con los usuarios de prueba.
- [ ] Ejecuté `mvnw.cmd test` / `./mvnw test` y las pruebas pasaron.
- [ ] Revisé y entiendo los comentarios de las partes difíciles (PBKDF2, escritura atómica, escape, cadena de hashes, hilos de Swing).
- [ ] Corregí o rechacé algo (anotar qué y por qué):

### Entrada 3
- Fecha y hora: 2026-11-15, desde las 19:10 aprox. (hora de Costa Rica)
- Autor: Andrey
- Herramienta: Claude Code (extensión de VS Code)
- Propósito: Después de integrar la rama de Sebastián: poder ejecutar el proyecto en VS Code,
  mostrar las contraseñas en `usuarios.txt` para la defensa, preparar el contexto para un video
  explicativo y agregar al módulo de amistades las solicitudes (aceptar / rechazar) y eliminar amigos.

Prompts de esta etapa (literales, en orden):

```text
sebastian arreglo lo que me habia hecho copilot y ya lo subio al main, yo ya hice pull request a mi rama. Puedes ayudarme? estoy viendo como correrlo con el run bat pero no me compila, no se si me hace falta algo? instala alguna extencion si hace falta o algo, no me sale la opcion para correr el codigo
```

```text
no necesito una extencion para correr el programa?
```

```text
Error: Main method not found in the file, please define the main method as: public static void main(String[] args) me tira este error }
```

```text
es mas recomendable tener el usuarios.txt donde traiga la informacion actual o tener un credencialesUsuario.txt y guardar la contraseña en este mismo? no quiero que generes nada, solo que respondas la pregunta porque no se si poner las credenciales en el mismo archivo usuarios.txt o mejor crear un archivo nuevo desde cero para las credenciales
```

```text
el proyecto es solo para fines academicos, no es necesario ocultar la contraseña en el archivo usuarios.txt. De hecho para la defensa del proyecto me sirve que se muestre la contraseña para poder iniciar session y enseñarselo al profesor. PERO NO HAGAS CAMBIOS TODAVIA, SOLO TE ESTOY INFORMANDO PARA QUE TENGAS UN POCO MAS DE CONTEXTO
```

```text
ya Sebastian termino su parte del proyecto y ya hice pull request de su rama a la mia, ahora... si voy a querer que hagas el cambio en usuarios.txt para que aparezca la contraseña directa y no el hash. Solamente quiero que toques eso, el resto dejalo igual, cambia solamente el formato del usuarios.txt para poder ver la contraseña desde ahí
```

```text
puedes hacerme un contexto de como funciona todo para hacer un video por aparte sobre como funciona toda la interfaz. Queremos hacer un video explicativo para pegarlo al inicio de la sesion y que los usuarios decidan si quieren verlo o no, pero vamos a ver si hacemos el video por separado para no gastar tokens.
```

```text
estaba probando el programa y me di cuenta de un error. Para empezar no puedo eliminar amigos una vez los añado, y luego otro error es que cuando "añado un amigo" se añade automaticamente, no deberia funcionar asi, si ana le manda una solicitud a maria, a maria deberia aparecerle un mensaje de "ana te envio una solicitud de amistad" y maria deberia poder elegir si aceptar la solicitud o cancelarla. Entonces añade esas dos cosas. 1. poder eliminar amigos. 2. que los usuarios puedan elegir si quieren aceptar la solicitud de amistad o no.
```

```text
actualizasste la bitacora de IA con los nuevos promts?
```

### Resumen de la respuesta generada
- **Ejecución**: el proyecto sí compilaba (`BUILD SUCCESS`). El problema era el entorno: en PowerShell
  hay que escribir `.\run.bat`, y la extensión "Java Run" (que compila archivos sueltos y no entiende
  proyectos Maven) daba el error "Main method not found". Se recomendó desactivar "Java Run" y
  "Oracle Java" (choca con la de Red Hat) y usar el Extension Pack for Java.
- **Credenciales**: se explicó que separar las credenciales en otro archivo no aporta seguridad real
  si ambos están en `data/`, y que agrega el riesgo de desincronizar dos archivos.
- **Contraseñas visibles** (decisión académica, para la defensa): `usuarios.txt` pasó a
  `id|nombre|usuario|contrasena|fechaISO`. Se agregó `HasheadorTextoPlano` y se cambió una línea en
  `EnsambladorServicios` (queda comentada la línea para volver a PBKDF2). Se conservaron los IDs y
  las fechas; la contraseña nunca se escribe en la auditoría.
- **Contexto para el video**: `docs/CONTEXTO_VIDEO.md` con cada pantalla, sus botones, reglas,
  atajos y un guion sugerido.
- **Solicitudes de amistad y eliminar amigos** (módulo de Sebastián, pedido por Andrey):
  - `SolicitudAmistad`, `SolicitudRecibida`, `ConsultaSolicitudes`, `RepositorioSolicitudes` y
    `RepositorioSolicitudesArchivoTexto` (archivo nuevo `data/solicitudes.txt` con las solicitudes
    PENDIENTES; `amistades.txt` no cambió de formato).
  - `GestorAmistades`: enviar, aceptar, rechazar y cancelar solicitudes, y eliminar amistades
    (disco primero y luego memoria; reglas validadas también al cargar). `GrafoAmistades.eliminar`
    quita la arista de las dos listas de adyacencia.
  - Interfaz: el buscador muestra "Enviar solicitud" / "Cancelar solicitud" / "Responder solicitud";
    pestaña nueva **Solicitudes (N)** con Aceptar y Rechazar; aviso al iniciar sesión "X te envió una
    solicitud de amistad" (Aceptar / Rechazar / Decidir después); botón **Eliminar amigo** con
    confirmación en "Mis amigos".
- **Pruebas**: 70 pruebas JUnit 5, todas pasan (8 nuevas para solicitudes y eliminar amigos). La
  interfaz nueva no se probó a mano en esta etapa.
- **Error de la IA corregido**: un comando `sed` dañó el README y `docs/CONTRATOS.md`; se restauraron
  desde git y se volvió a hacer el cambio a mano.

Archivos creados: `seguridad/HasheadorTextoPlano.java`, `amistades/SolicitudAmistad.java`,
`amistades/SolicitudRecibida.java`, `amistades/ConsultaSolicitudes.java`,
`amistades/RepositorioSolicitudes.java`, `amistades/RepositorioSolicitudesArchivoTexto.java`,
`ui/PanelSolicitudes.java`, `ui/AccionesAmistad.java`, `docs/CONTEXTO_VIDEO.md`.
Modificados: `EnsambladorServicios.java`, `EnsambladorSocial.java`,
`persistencia/RepositorioUsuariosArchivoTexto.java`, `amistades/GestorAmistades.java`,
`amistades/GrafoAmistades.java`, `amistades/ServicioAmistades.java`, `amistades/BuscadorUsuarios.java`,
`amistades/ResultadoBusqueda.java`, `ui/VentanaPrincipal.java`, `ui/PanelBuscador.java`,
`ui/PanelAmigos.java`, `ui/Dialogos.java`, `data/usuarios.txt`, `README.md`, `docs/CONTRATOS.md` y
las pruebas `PruebasRepositorioUsuariosArchivoTexto`, `PruebasGestorUsuarios`,
`PruebasGestorAmistades`, `PruebasBuscadorUsuarios` y `PruebasGestorPublicaciones`.

### Revisión humana
- [ ] Ejecuté la app con `.\run.bat` e inicié sesión viendo la contraseña en `usuarios.txt`.
- [ ] Probé: Ana envía solicitud a Maria → Maria ve el aviso al entrar → acepta o rechaza.
- [ ] Probé cancelar una solicitud enviada y eliminar un amigo.
- [ ] Sebastián revisó los cambios en su módulo de amistades.
- [ ] Ejecuté `mvnw.cmd test` y las pruebas pasaron.
- [ ] Corregí o rechacé algo (anotar qué y por qué):

## Bitácora de IA (Sebastián)

### Entrada 1
- Fecha y hora: 2026-11-15 19:30 (hora de Costa Rica)
- Autor: Sebastián
- Herramienta: Claude Code (extensión de VS Code)
- Propósito: Implementar la parte de Sebastián según la sección 5 del prompt y el PDF: módulo social
  (buscador, "Agregar como amigo", lista de amigos, grafo de amistades, `amistades.txt`), módulo de
  publicaciones (publicar, muro cronológico propio + amigos, `publicaciones.txt`, datos de ejemplo) y
  la ventana principal real con navegación entre muro, buscador y amigos.

Prompt de esta etapa (literal):

```text
ahora haz la parte de Sebastian justo como viene el promp la solicitud, hazlo de manera correcta sin fallo, luego me indicas y me explicas lo que hiciste
```

### Resumen de la respuesta generada
- **Amistades** (`una.social.amistades`): `GrafoAmistades` (grafo no dirigido con listas de
  adyacencia), `GestorAmistades` (reglas: no a sí mismo, no repetida, el usuario debe existir;
  guarda disco y luego memoria), `RepositorioAmistadesArchivoTexto`, `BuscadorUsuarios` (sin
  mayúsculas ni tildes), interfaces pequeñas `ConsultaAmistades`, `ServicioAmistades`,
  `ServicioBusqueda`.
- **Publicaciones** (`una.social.publicaciones`): `Publicacion` (inmutable, IDs P0001...),
  `HistorialPublicaciones` (lista dinámica en orden cronológico; muro recorriendo de atrás hacia
  adelante), `GestorPublicaciones`, `RepositorioPublicacionesArchivoTexto`.
- **Persistencia compartida**: `ArchivoRegistros` (lectura/escritura genérica con omisión auditada
  de líneas corruptas).
- **Interfaz**: `VentanaPrincipal` con barra superior y pestañas Muro / Buscar estudiantes / Mis
  amigos (`PanelMuro`, `PanelBuscador`, `PanelAmigos`), con SwingWorker, errores en línea y alertas.
- **Integración**: `EnsambladorSocial`, carga en dos fases en `Main` y registro de la
  `VentanaPrincipal` en la `FabricaVentanaPrincipal`. Datos de ejemplo en `data/amistades.txt` y
  `data/publicaciones.txt` (y `HerramientaDatosSemillaSocial` para recrearlos).
- **Pruebas**: 24 pruebas nuevas (62 en total, todas pasan). Prueba de humo de la UI completa y
  capturas de pantalla revisadas.

Archivos creados: `src/main/java/una/social/amistades/**`, `src/main/java/una/social/publicaciones/**`,
`EnsambladorSocial.java`, `persistencia/ArchivoRegistros.java`, `herramientas/HerramientaDatosSemillaSocial.java`,
`excepciones/ExcepcionAmistadInvalida.java`, `excepciones/ExcepcionPublicacionInvalida.java`,
`ui/VentanaPrincipal.java`, `ui/PanelMuro.java`, `ui/PanelBuscador.java`, `ui/PanelAmigos.java`,
`ui/SeccionPrincipal.java`, `ui/FilaUsuario.java`, `ui/ListaTarjetas.java`, `ui/AvatarUsuario.java`,
`ui/TextosUi.java`, `ui/tema/AreaTextoTema.java`, `data/amistades.txt`, `data/publicaciones.txt` y las
pruebas en `src/test/java/una/social/{amistades,publicaciones}/` y `soporte/DirectorioEnMemoria.java`.
Modificados: `Main.java`, `README.md`, `docs/CONTRATOS.md`, `docs/UML.md`.

### Revisión humana
- [ ] Revisé el grafo de amistades y sé explicar la lista de adyacencia.
- [ ] Revisé cómo se arma el muro (orden y filtro por amigos).
- [ ] Ejecuté la app, publiqué, agregué un amigo y vi el muro cruzado.
- [ ] Ejecuté `mvnw.cmd test` y las pruebas pasaron.
- [ ] Corregí o rechacé algo (anotar qué y por qué):

### Entrada 2
- Fecha y hora: 2026-11-15 22:13 (hora de Costa Rica)
- Autor: Sebastián
- Herramienta: Claude Code (extensión de VS Code)
- Propósito: Ampliar la red social con solicitudes (cola), sugerencias (BFS), deshacer/editar
  (pila), me gusta, comentarios, biografía, compartir, privacidad, dos vistas del muro, perfiles,
  eliminar amigo, borrar publicación, bloqueos, editar nombre/contraseña y anuncios, revisando lo
  que ya existía para no repetirlo ni romperlo.

Prompt de esta etapa (literal; incluye la nota que se agregó al final):

```text
ROL
Eres un ingeniero de software senior en Java. Vas a AMPLIAR la red social "Social UNA" que ya existe en este repositorio (rama "Sebastian"). El proyecto ya funciona: login/registro (parte de Andrey) y amistades, publicaciones, muro y ventana principal (parte de Sebastián). Antes de escribir código, lee el README.md, docs/CONTRATOS.md, docs/UML.md y el código existente. NO rompas nada de lo que ya funciona: las 62 pruebas actuales deben seguir pasando.

1. CONTEXTO TÉCNICO QUE DEBES RESPETAR
- Java 17 (una sola versión), Swing, Maven Wrapper (mvnw.cmd / ./mvnw). Sin librerías externas salvo JUnit 5 para pruebas.
- Nombres de clases, métodos, variables y archivos en ESPAÑOL (excepto Main).
- Persistencia SOLO en archivos .txt dentro de data/, formato propio delimitado por "|", con escape (\|, \\, \n, \r), líneas con # como comentarios, escritura atómica. Reutiliza AlmacenArchivoTexto y ArchivoRegistros; no dupliques esa lógica. Líneas corruptas o con referencias inválidas: se omiten, se auditan y la carga continúa.
- Patrón existente: interfaz pequeña de servicio + Gestor que la implementa + Repositorio (interfaz + ...ArchivoTexto) + estructura de datos en memoria. Los gestores implementan ModuloPersistente y se registran en Main (EnsambladorSocial). Guardar primero en disco y luego en memoria. Flag "datosCargados" para no sobrescribir archivos si la carga falló.
- SOLID real: dependencias inyectadas por constructor, interfaces pequeñas (ISP), sin singletons.
- Excepciones: heredar de ExcepcionSocialUna; mensajes amigables al usuario y detalle técnico a la auditoría. Nunca catch vacíos.
- assert solo para invariantes internas; excepciones para validar datos del usuario o de archivos.
- Auditoría (RegistroAuditoria): registra cada acción nueva (por ejemplo SOLICITUD_ENVIADA, ME_GUSTA, COMENTARIO_CREADO, PUBLICACION_EDITADA, USUARIO_BLOQUEADO...). Nunca registres contraseñas, hashes ni el contenido completo de publicaciones o comentarios.
- UI: usa TemaUi y sus componentes, TareaEnSegundoPlano (SwingWorker) para todo lo que lea o escriba archivos, Dialogos para alertas, errores en línea en rojo, confirmación antes de acciones destructivas, etiquetas de texto plano (TextosUi) para datos escritos por usuarios.
- Comentarios OBLIGATORIOS en español, igual que el resto del proyecto: cabecera en cada archivo (qué hace, rol, principio SOLID, colaboradores), Javadoc en toda clase, método, constructor y atributo, comentarios del PORQUÉ, cada try/catch y cada assert explicados. Explica con lenguaje sencillo los algoritmos (BFS, pila, cola) para poder defenderlos.

2. FUNCIONES A IMPLEMENTAR

2.1 Personas que quizás conozcas
- Recorrido en anchura (BFS) a 2 niveles sobre el grafo de amistades: sugerir amigos de mis amigos.
- Contar amigos en común con un HashMap y ordenar por más amigos en común (desempate alfabético). Mostrar como máximo 10.
- Excluir: a mí mismo, a mis amigos, a usuarios bloqueados (en cualquier sentido) y a quienes ya tienen una solicitud pendiente conmigo.
- UI: tarjeta "Personas que quizás conozcas" con "N amigos en común" y botón "Enviar solicitud".

2.2 Deshacer y editar la última publicación
- Deshacer: una PILA (LIFO) por usuario con sus publicaciones de la sesión; "Deshacer última publicación" borra la más reciente que publicó (con confirmación).
- Editar: el usuario solo puede editar SU publicación más reciente. Se guarda la fecha de edición y se muestra "(editada)". Mismas validaciones que al publicar.

2.3 Solicitudes de amistad (reemplaza el "Agregar como amigo" directo)
- Enviar, aceptar, rechazar y cancelar solicitudes. Las solicitudes recibidas se guardan en una COLA (FIFO) por usuario: se muestran en el orden en que llegaron.
- Reglas: no a uno mismo, no si ya son amigos, no duplicadas, no si hay bloqueo. Si A le envía a B y B ya le había enviado a A, se aceptan automáticamente.
- Al aceptar se crea la amistad en el grafo existente. Las amistades actuales se conservan.
- Persistencia: data/solicitudes.txt.
- UI: pestaña "Solicitudes" con contador de pendientes, botones Aceptar y Rechazar, y lista de enviadas con Cancelar. En el buscador y en los perfiles, el botón muestra el estado: "Enviar solicitud", "Solicitud enviada", "Responder solicitud", "Amigos".

2.4 Me gusta
- Un "me gusta" por usuario y publicación; pulsar de nuevo lo quita (alternar).
- Estructura: HashMap<idPublicacion, Set<idUsuario>>.
- En la publicación SOLO se muestra el número de "me gusta", nunca quién los dio.
- Persistencia: data/megusta.txt.

2.5 Comentarios
- Comentar publicaciones visibles para mí. Máximo 300 caracteres. Se muestran del más antiguo al más reciente bajo la publicación (con "Ver comentarios (N)").
- Pueden borrar un comentario su autor o el autor de la publicación.
- Persistencia: data/comentarios.txt.

2.6 Biografía del perfil
- Texto opcional de máximo 160 caracteres, editable desde mi perfil.
- NO cambies el formato de usuarios.txt: guarda la biografía en data/perfiles.txt (idUsuario|biografia).

2.7 Compartir publicaciones
- Solo se pueden compartir publicaciones PÚBLICAS de otras personas. No se puede compartir la propia ni la misma publicación dos veces.
- Lo compartido aparece en mi perfil y en los muros como "X compartió una publicación de Y", mostrando la original.
- Si la original se borra, lo compartido muestra "La publicación original ya no está disponible".
- Persistencia: data/compartidos.txt.

2.8 Privacidad de publicaciones
- Cada publicación es "Pública" o "Solo amigos" (selector al publicar; por defecto "Solo amigos").
- publicaciones.txt agrega una columna de privacidad y otra de fecha de edición. COMPATIBILIDAD: el lector debe aceptar también las líneas antiguas de 4 columnas (tratarlas como "Solo amigos" y sin edición) y al guardar escribirlas en el formato nuevo. Agrega pruebas de esta migración.

2.9 Muro con dos vistas
- En la sección de publicaciones, un selector con dos opciones:
  - "Muro general": todas las publicaciones PÚBLICAS de todos los usuarios (y lo compartido), de la más reciente a la más antigua.
  - "Mis amigos": mis publicaciones y las de mis amigos (públicas y solo amigos) y lo compartido por ellos, de la más reciente a la más antigua.
- En ambas vistas se excluye todo lo de usuarios bloqueados.

2.10 Perfiles
- Mi perfil: nombre, @usuario, ID, biografía, y SOLO mis publicaciones y mis compartidos (más recientes primero). Desde aquí: editar biografía, editar nombre, cambiar contraseña, deshacer o editar mi última publicación y ver mis bloqueados.
- Perfil de otro usuario (al hacer clic en su nombre en el muro, buscador, amigos o sugerencias): nombre, @usuario, ID, biografía, cantidad de amigos en común, botón de estado de amistad, "Eliminar amigo" (si lo es) y "Bloquear". Sus publicaciones: las públicas; y si somos amigos, también las de "Solo amigos".

2.11 Eliminar amigo, borrar publicación y bloquear
- Eliminar amigo: con confirmación; quita la arista del grafo y se guarda.
- Borrar publicación propia (cualquiera, no solo la última): con confirmación; borra también sus "me gusta" y comentarios. Lo que otros compartieron de ella queda como "no disponible".
- Bloquear usuario: con confirmación; elimina la amistad, cancela solicitudes entre ambos y oculta mutuamente publicaciones, comentarios, búsqueda, sugerencias y perfiles. Lista de bloqueados con opción "Desbloquear". Persistencia: data/bloqueos.txt.

2.12 Editar nombre y contraseña
- Esto toca el módulo de usuarios (Andrey): agrégalo mediante una interfaz nueva y pequeña (por ejemplo ServicioCuenta) implementada por GestorUsuarios, sin romper las interfaces existentes.
- Cambiar nombre: mismas validaciones que en el registro.
- Cambiar contraseña: pide la contraseña actual, la nueva y su confirmación; mismas reglas, PBKDF2 con sal NUEVA, contraseñas siempre como char[] y borradas con Arrays.fill. Audita sin datos sensibles.

2.13 Sección de anuncios (pública)
- Pestaña "Anuncios": cualquier usuario puede publicar un anuncio (título de máximo 80 caracteres y texto de máximo 500). Todos los ven, del más reciente al más antiguo. Solo su autor puede borrarlo. Se ocultan los de usuarios bloqueados.
- Persistencia: data/anuncios.txt.

3. DATOS DE EJEMPLO
- Actualiza los datos de ejemplo (y HerramientaDatosSemillaSocial) para demostrar todo: publicaciones públicas y solo amigos, "me gusta", comentarios, un compartido, una solicitud pendiente, biografías y 2 anuncios. Usa los usuarios U001 Ana, U002 Luis y U003 Maria.
- Cada archivo nuevo lleva un encabezado con # que explique sus columnas.

4. PRUEBAS (JUnit 5, nombres "Pruebas*")
Como mínimo, para cada función:
- reglas válidas e inválidas;
- persistencia (guardar y recargar);
- archivo corrupto;
- BFS de sugerencias (incluido el orden por amigos en común);
- pila de deshacer;
- cola de solicitudes;
- "me gusta" que alterna;
- filtros de privacidad y de bloqueo en ambos muros;
- migración del formato antiguo de publicaciones;
- borrado en cascada;
- cambio de contraseña (la vieja deja de servir y la nueva funciona).
Las 62 pruebas existentes deben seguir pasando.

5. DOCUMENTACIÓN
- Actualiza README.md (funciones, archivos de data/, estructuras de datos, decisiones), docs/CONTRATOS.md y docs/UML.md (diagramas Mermaid).
- Agrega una nueva entrada en "Bitácora de IA (Sebastián)" del README con este prompt completo y literal, el resumen de lo hecho, los archivos creados y modificados, y casillas de "Revisión humana".

6. FORMA DE TRABAJAR
- Primero muéstrame un plan breve: paquetes, clases nuevas, cambios de formato de archivos y orden de implementación por fases.
- Implementa por fases. Al terminar cada fase, compila y corre todas las pruebas con mvnw.cmd test, y corrige lo que falle antes de seguir.
- Fases sugeridas:
  1. Privacidad + dos vistas del muro.
  2. Me gusta + comentarios.
  3. Solicitudes + eliminar amigo + bloqueos.
  4. Sugerencias (BFS).
  5. Perfiles + biografía + editar nombre y contraseña.
  6. Deshacer/editar + compartir.
  7. Anuncios.
  8. Datos de ejemplo + documentación.
- Revisa la interfaz con capturas de pantalla y haz una prueba del flujo completo.
- Si algo es ambiguo, elige la opción más simple y segura, documéntala en el README y avísame.
- NO hagas commit ni push hasta que yo lo revise y te lo pida.
- Al final, dame un resumen con: qué se hizo, cómo probarlo paso a paso en la app y qué decisiones debo saber defender.

*NOTA IMPORTANTE: Haz estos cambios, pero ya Andrey habia implementado unos que vienen en la lista, entonces verifique los archivos del codigo y haz los faltantes que te pido, REVISA QUE TODO ESTE CORRECTO Y QUE TODO ESTE EN ORDEN Y SIN BUGS
```

### Resumen de la respuesta generada
- **Revisión previa**: Andrey ya había hecho (commit 87d1b31) las solicitudes básicas (enviar, aceptar,
  rechazar, cancelar), "Eliminar amigo" y la pestaña Solicitudes. Se conservaron y se completaron:
  cola FIFO real (`BandejaSolicitudes`), aceptación automática si ambos se envían, regla de bloqueo
  y lista de solicitudes enviadas con Cancelar.
- **Nuevos paquetes**: `bloqueos/`, `interacciones/` (me gusta y comentarios), `perfiles/`,
  `anuncios/`. Nuevas piezas comunes: `GestorPersistenteBase` (método plantilla con carga,
  guardado y auditoría), `ExcepcionReglaNegocio`, `ModulosSociales`, `ServiciosSociales`,
  `AccionesUi`.
- **Publicaciones**: `Privacidad`, columnas nuevas en `publicaciones.txt` con lectura del formato
  viejo (y prueba de migración), muro "Mis amigos" / "Muro general" (mezcla de listas ordenadas),
  pila de deshacer, editar la última, borrar con cascada (Observador) y compartidos con "no
  disponible".
- **Sugerencias**: `SugeridorAmistades` (BFS a 2 niveles + `HashMap` de amigos en común).
- **Cuenta** (módulo de Andrey, sin romper sus interfaces): `ServicioCuenta` implementada por
  `GestorUsuarios` (editar nombre y cambiar contraseña con `char[]` y `Arrays.fill`). Se respetó
  la decisión de Andrey de contraseñas en texto plano; con PBKDF2 se genera sal nueva (probado).
- **Interfaz**: pestañas Anuncios y Mi perfil; ventana de perfil de otro usuario (clic en cualquier
  nombre); tarjetas con Me gusta, Ver comentarios, Compartir, Editar y Borrar; diálogos de
  comentarios, cambio de contraseña y bloqueados; selector de privacidad y de vista del muro.
- **Datos de ejemplo** regenerados con la herramienta de semilla (todos los archivos nuevos, con
  encabezado `#`).
- **Pruebas**: de 70 a **106** (todas pasan). Además, revisión de la interfaz con capturas de
  pantalla de todas las pestañas y diálogos, y una prueba del flujo completo automatizada
  (rechazar solicitud → sugerencia → publicar pública → me gusta → deshacer con cascada → bloquear →
  releer de disco → auditoría sin contraseñas ni textos).
- **Corregido durante la revisión**: los avisos de éxito (deshacer, compartir, eliminar amigo,
  enviar solicitud) ahora se muestran después de refrescar la lista, para que detrás del aviso ya
  se vea el estado nuevo; el botón de comentarios dice "Ver comentarios (N)" como pide el prompt.
- **Ambigüedades resueltas** (lo más simple y seguro): ver "Decisiones tomadas", puntos 8 a 18.

Archivos creados:
- `data/anuncios.txt`
- `data/bloqueos.txt`
- `data/comentarios.txt`
- `data/compartidos.txt`
- `data/megusta.txt`
- `data/perfiles.txt`
- `src/main/java/una/social/ModulosSociales.java`
- `src/main/java/una/social/amistades/BandejaSolicitudes.java`
- `src/main/java/una/social/amistades/ResultadoEnvioSolicitud.java`
- `src/main/java/una/social/amistades/ServicioSugerencias.java`
- `src/main/java/una/social/amistades/SolicitudEnviada.java`
- `src/main/java/una/social/amistades/Sugerencia.java`
- `src/main/java/una/social/amistades/SugeridorAmistades.java`
- `src/main/java/una/social/anuncios/Anuncio.java`
- `src/main/java/una/social/anuncios/EntradaAnuncio.java`
- `src/main/java/una/social/anuncios/GestorAnuncios.java`
- `src/main/java/una/social/anuncios/RepositorioAnuncios.java`
- `src/main/java/una/social/anuncios/RepositorioAnunciosArchivoTexto.java`
- `src/main/java/una/social/anuncios/ServicioAnuncios.java`
- `src/main/java/una/social/anuncios/TablonAnuncios.java`
- `src/main/java/una/social/bloqueos/Bloqueo.java`
- `src/main/java/una/social/bloqueos/ConsultaBloqueos.java`
- `src/main/java/una/social/bloqueos/GestorBloqueos.java`
- `src/main/java/una/social/bloqueos/OyenteBloqueo.java`
- `src/main/java/una/social/bloqueos/RegistroBloqueos.java`
- `src/main/java/una/social/bloqueos/RepositorioBloqueos.java`
- `src/main/java/una/social/bloqueos/RepositorioBloqueosArchivoTexto.java`
- `src/main/java/una/social/bloqueos/ServicioBloqueos.java`
- `src/main/java/una/social/excepciones/ExcepcionReglaNegocio.java`
- `src/main/java/una/social/interacciones/Comentario.java`
- `src/main/java/una/social/interacciones/ComentarioVisible.java`
- `src/main/java/una/social/interacciones/EstadoMeGusta.java`
- `src/main/java/una/social/interacciones/GestorComentarios.java`
- `src/main/java/una/social/interacciones/GestorMeGusta.java`
- `src/main/java/una/social/interacciones/MeGusta.java`
- `src/main/java/una/social/interacciones/RegistroComentarios.java`
- `src/main/java/una/social/interacciones/RegistroMeGusta.java`
- `src/main/java/una/social/interacciones/RepositorioComentarios.java`
- `src/main/java/una/social/interacciones/RepositorioComentariosArchivoTexto.java`
- `src/main/java/una/social/interacciones/RepositorioMeGusta.java`
- `src/main/java/una/social/interacciones/RepositorioMeGustaArchivoTexto.java`
- `src/main/java/una/social/interacciones/ServicioComentarios.java`
- `src/main/java/una/social/interacciones/ServicioMeGusta.java`
- `src/main/java/una/social/perfiles/GestorPerfiles.java`
- `src/main/java/una/social/perfiles/Perfil.java`
- `src/main/java/una/social/perfiles/RepositorioPerfiles.java`
- `src/main/java/una/social/perfiles/RepositorioPerfilesArchivoTexto.java`
- `src/main/java/una/social/perfiles/ServicioPerfiles.java`
- `src/main/java/una/social/persistencia/GestorPersistenteBase.java`
- `src/main/java/una/social/publicaciones/Compartido.java`
- `src/main/java/una/social/publicaciones/ConsultaPublicaciones.java`
- `src/main/java/una/social/publicaciones/OyenteBorradoPublicacion.java`
- `src/main/java/una/social/publicaciones/Privacidad.java`
- `src/main/java/una/social/publicaciones/RegistroCompartidos.java`
- `src/main/java/una/social/publicaciones/RepositorioCompartidos.java`
- `src/main/java/una/social/publicaciones/RepositorioCompartidosArchivoTexto.java`
- `src/main/java/una/social/servicio/ServicioCuenta.java`
- `src/main/java/una/social/ui/AccionesUi.java`
- `src/main/java/una/social/ui/CabeceraPerfil.java`
- `src/main/java/una/social/ui/DialogoBloqueados.java`
- `src/main/java/una/social/ui/DialogoCambioContrasena.java`
- `src/main/java/una/social/ui/DialogoComentarios.java`
- `src/main/java/una/social/ui/DialogosTexto.java`
- `src/main/java/una/social/ui/EstadoRelacion.java`
- `src/main/java/una/social/ui/PanelAnuncios.java`
- `src/main/java/una/social/ui/PanelMiPerfil.java`
- `src/main/java/una/social/ui/ServiciosSociales.java`
- `src/main/java/una/social/ui/TarjetasPublicacion.java`
- `src/main/java/una/social/ui/VentanaPerfil.java`
- `src/main/java/una/social/ui/VistaPublicacion.java`
- `src/test/java/una/social/amistades/PruebasSolicitudesYBloqueos.java`
- `src/test/java/una/social/amistades/PruebasSugerencias.java`
- `src/test/java/una/social/herramientas/PruebasDatosSemillaSocial.java`
- `src/test/java/una/social/interacciones/PruebasInteracciones.java`
- `src/test/java/una/social/perfiles/PruebasPerfilesYAnuncios.java`
- `src/test/java/una/social/publicaciones/PruebasPublicacionesAvanzadas.java`
- `src/test/java/una/social/servicio/PruebasCuenta.java`
- `src/test/java/una/social/soporte/EntornoSocial.java`

Archivos modificados:
- `README.md`
- `data/amistades.txt`
- `data/publicaciones.txt`
- `data/solicitudes.txt`
- `docs/CONTEXTO_VIDEO.md`
- `docs/CONTRATOS.md`
- `docs/UML.md`
- `src/main/java/una/social/EnsambladorSocial.java`
- `src/main/java/una/social/Main.java`
- `src/main/java/una/social/amistades/BuscadorUsuarios.java`
- `src/main/java/una/social/amistades/GestorAmistades.java`
- `src/main/java/una/social/amistades/ServicioAmistades.java`
- `src/main/java/una/social/excepciones/ExcepcionAmistadInvalida.java`
- `src/main/java/una/social/excepciones/ExcepcionPublicacionInvalida.java`
- `src/main/java/una/social/herramientas/HerramientaDatosSemillaSocial.java`
- `src/main/java/una/social/modelo/CampoFormulario.java`
- `src/main/java/una/social/modelo/CatalogoUsuarios.java`
- `src/main/java/una/social/persistencia/ArchivoRegistros.java`
- `src/main/java/una/social/publicaciones/EntradaMuro.java`
- `src/main/java/una/social/publicaciones/GestorPublicaciones.java`
- `src/main/java/una/social/publicaciones/HistorialPublicaciones.java`
- `src/main/java/una/social/publicaciones/Publicacion.java`
- `src/main/java/una/social/publicaciones/RepositorioPublicacionesArchivoTexto.java`
- `src/main/java/una/social/publicaciones/ServicioPublicaciones.java`
- `src/main/java/una/social/servicio/GestorUsuarios.java`
- `src/main/java/una/social/servicio/ValidadorDatosUsuario.java`
- `src/main/java/una/social/sesion/ContextoSesion.java`
- `src/main/java/una/social/ui/AccionesAmistad.java`
- `src/main/java/una/social/ui/FilaUsuario.java`
- `src/main/java/una/social/ui/PanelAmigos.java`
- `src/main/java/una/social/ui/PanelBuscador.java`
- `src/main/java/una/social/ui/PanelMuro.java`
- `src/main/java/una/social/ui/PanelSolicitudes.java`
- `src/main/java/una/social/ui/TextosUi.java`
- `src/main/java/una/social/ui/VentanaPrincipal.java`
- `src/test/java/una/social/amistades/PruebasBuscadorUsuarios.java`
- `src/test/java/una/social/amistades/PruebasGestorAmistades.java`
- `src/test/java/una/social/publicaciones/PruebasGestorPublicaciones.java`

### Revisión humana
- [ ] Revisé el BFS de sugerencias y sé explicar por qué cuenta los amigos en común.
- [ ] Revisé la cola de solicitudes y la pila de deshacer.
- [ ] Revisé la privacidad, las dos vistas del muro y el bloqueo (que no se vea nada del bloqueado).
- [ ] Revisé el borrado en cascada y los compartidos "no disponibles".
- [ ] Ejecuté la app y seguí la "Demostración sugerida" del README.
- [ ] Ejecuté `mvnw.cmd test` y las 106 pruebas pasaron.
- [ ] Corregí o rechacé algo (anotar qué y por qué):
