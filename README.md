# Social UNA - Proyecto 3

Red social estudiantil de la Universidad Nacional (Costa Rica), hecha en **Java 17 + Swing**.
Los usuarios se registran, inician sesión, buscan estudiantes, los agregan como amigos y publican
en un muro cronológico. Toda la información vive en memoria (estructuras de datos) y se guarda en
archivos de texto `.txt` dentro de `data/` (sin bases de datos).

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
  3 usuarios de prueba automáticamente.

## Usuarios de prueba (para Sebastián: estos IDs son fijos)

| ID | Nombre | Usuario | Contraseña |
|----|--------|---------|------------|
| U001 | Ana Mora | `ana` | `ana12345` |
| U002 | Luis Rojas | `luis` | `luis12345` |
| U003 | Maria Vargas | `maria` | `maria12345` |

`data/amistades.txt` y `data/publicaciones.txt` (módulo de Sebastián) deben referenciar estos IDs.

## Archivos de datos (`data/`)

Todos son texto plano UTF-8, un registro por línea, columnas separadas por `|`. Las líneas que
empiezan con `#` son comentarios y el programa las ignora. Dentro de un campo se escapan:
`\|` (barra vertical), `\\` (barra invertida), `\n` y `\r` (saltos de línea).

| Archivo | Formato | Dueño |
|---------|---------|-------|
| `usuarios.txt` | `id\|nombre\|usuario\|salBase64\|hashBase64\|fechaISO` | Andrey |
| `auditoria.log` | `fechaISO\|nivel\|actor\|accion\|detalle\|hashAnterior\|hashActual` (lo genera la app; no se versiona) | Andrey |
| `amistades.txt` | lo define Sebastián (ver `docs/CONTRATOS.md`) | Sebastián |
| `publicaciones.txt` | lo define Sebastián (ver `docs/CONTRATOS.md`) | Sebastián |

Una línea dañada de un archivo de datos **no tumba la app**: se omite, se registra en la auditoría
con su número de línea y se cargan las demás.

## Estructura del código (parte de Andrey)

```
src/main/java/una/social/
├── Main.java                     arranque y "raíz de composición" (único nombre en inglés)
├── EnsambladorServicios.java     crea y conecta las implementaciones concretas
├── auditoria/      bitácora append-only con hash SHA-256 encadenado y su verificador
├── excepciones/    jerarquía ExcepcionSocialUna y subclases
├── herramientas/   HerramientaDatosSemilla (3 usuarios de prueba)
├── modelo/         Usuario (inmutable), CatalogoUsuarios (ArrayList + HashMap), CampoFormulario
├── persistencia/   AlmacenArchivoTexto (compartido), repositorio de usuarios, ModuloPersistente
├── seguridad/      PBKDF2 + sal, control de intentos fallidos
├── servicio/       interfaces pequeñas (DirectorioUsuarios, ServicioAutenticacion,
│                   ServicioRegistro) y GestorUsuarios
├── sesion/         ContextoSesion (usuario activo)
└── ui/             ventanas, navegación, diálogos, manejador global de errores
    └── tema/       TemaUi y componentes con estilo (compartido)
src/test/java/...   38 pruebas JUnit 5 + oyente que audita las ejecuciones (nivel TEST)
docs/               CONTRATOS.md (integración con Sebastián) y UML.md (diagrama Mermaid)
```

## Decisiones tomadas (y por qué)

1. **Una sola versión de Java (17) y Maven Wrapper**: solo hay que instalar un JDK; no hace falta
   instalar Maven ni extensiones extra.
2. **Nombres en español** en clases, métodos y archivos (excepto `Main`), para ser consistentes
   con los comentarios. Los paquetes también están en español (`persistencia`, `ui.tema`...).
3. **Reglas de validación** (el enunciado pide "campos vacíos"; se eligió lo más simple y seguro):
   nombre ≤ 60 caracteres; usuario de 3 a 20 caracteres con letras sin tilde, números, `.`, `-`
   o `_`; contraseña de 6 a 128 caracteres, no solo espacios; confirmación igual.
4. **Bloqueo**: 5 intentos fallidos → 5 minutos de bloqueo, en memoria (se olvida al reiniciar).
5. **PBKDF2WithHmacSHA256 con 310 000 iteraciones** y sal de 16 bytes. Si se cambia el número de
   iteraciones, los hashes guardados dejan de coincidir.
6. **El log de pruebas** se escribe aparte, en `target/auditoria-pruebas.log`, con nivel TEST.
7. **Limitación conocida de la auditoría**: borrar las ÚLTIMAS líneas del log no se puede detectar
   solo con la cadena de hashes (lo que queda sigue siendo una cadena válida). Editar, borrar o
   insertar líneas intermedias sí se detecta.

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
- Autor: _(completar: persona que solicitó esta etapa)_
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
