# Contexto para el video explicativo de Social UNA

Este documento describe la interfaz de Social UNA tal como funciona hoy, pantalla por pantalla,
para preparar un video corto que se ofrecerá al iniciar sesión (opcional para el usuario).

## Qué es Social UNA (para la introducción del video)

Red social estudiantil de la Universidad Nacional (Costa Rica). Cada estudiante crea su cuenta,
inicia sesión, busca a otros estudiantes (y recibe sugerencias de "Personas que quizás conozcas"),
les envía solicitudes de amistad, publica mensajes públicos o solo para amigos, da "me gusta",
comenta, comparte, tiene un perfil con biografía y puede publicar anuncios para toda la comunidad.

## Usuarios para grabar la demostración

| ID | Nombre | Usuario | Contraseña |
|----|--------|---------|------------|
| U001 | Ana Mora | `ana` | `ana12345` |
| U002 | Luis Rojas | `luis` | `luis12345` |
| U003 | Maria Vargas | `maria` | `maria12345` |

Datos iniciales: amistades **Ana ↔ Luis** y **Luis ↔ Maria**; Ana y Maria **no** son amigas y
**Maria ya le envió una solicitud a Ana** (pendiente). Hay 6 publicaciones (2 por usuario; la
primera de Ana y la primera de Maria son públicas), "me gusta", 2 comentarios, un compartido de
Luis, biografías y 2 anuncios.

Para empezar la grabación con los datos limpios: cerrar la app, borrar de `data/` todos los `.txt`
**menos** `usuarios.txt` y volver a abrirla (los datos de ejemplo se recrean solos). Si los datos
ya están en git, también sirve `git restore data/`.

---

## 1. Pantalla de inicio de sesión ("Social UNA - Iniciar sesión")

- Título **Social UNA** y el lema "La red social estudiantil de la Universidad Nacional".
- Campo **Usuario** (ejemplo en gris: "ej. ana").
- Campo **Contraseña**, con la casilla **Mostrar contraseña** para ver lo que se escribe.
- Botón **Iniciar sesión**. Mientras valida, aparece "Verificando…".
- Enlace **Crear una cuenta nueva**, que lleva a la pantalla de registro.

Comportamiento a mencionar:
- El usuario no distingue mayúsculas (`Ana` = `ana`).
- Si el usuario o la contraseña están mal, el mensaje es siempre el mismo ("No se pudo iniciar
  sesión"), sin decir cuál de los dos falló. Así nadie puede adivinar qué usuarios existen.
- Después de **5 intentos fallidos**, la cuenta queda **bloqueada 5 minutos** ("Acceso bloqueado").

## 2. Pantalla de registro ("Social UNA - Crear cuenta")

- Texto: "Únete a Social UNA. Tu ID (U001, U002…) se asigna automáticamente."
- Campos: **Nombre completo** (ej. Ana Mora), **Usuario** (ej. ana, sin espacios),
  **Contraseña** (mínimo 6 caracteres) y **Confirmar contraseña**, más la casilla
  **Mostrar contraseñas**.
- Botón **Crear cuenta** (muestra "Creando cuenta…") y enlace **Ya tengo cuenta: iniciar sesión**.
- Al terminar aparece **¡Cuenta creada!** con "Bienvenido(a), …", el **ID asignado** (por
  ejemplo U004) y la indicación de que ya puede iniciar sesión.

Reglas de validación (aparecen como mensajes si algo está mal):
- Nombre: obligatorio, máximo 60 caracteres.
- Usuario: de 3 a 20 caracteres; letras sin tilde, números, `.`, `-` o `_`; no puede repetirse.
- Contraseña: de 6 a 128 caracteres, no solo espacios, y la confirmación debe ser igual.

## 3. Ventana principal ("Social UNA - <nombre>")

- Arriba: el nombre del usuario activo, su avatar y el botón **Cerrar sesión**.
- Seis pestañas, cada una con atajo de teclado:

| Pestaña | Atajo | Para qué sirve |
|---------|-------|----------------|
| **Muro** | Alt+M | Publicar y ver el muro ("Mis amigos" o "Muro general") |
| **Anuncios** | Alt+N | Anuncios públicos para toda la comunidad |
| **Buscar estudiantes** | Alt+B | Sugerencias "Personas que quizás conozcas" y buscador |
| **Mis amigos** | Alt+A | Lista de amigos y "Eliminar amigo" |
| **Solicitudes** | Alt+S | Recibidas (Aceptar / Rechazar) y enviadas (Cancelar); muestra cuántas hay, ej. "Solicitudes (1)" |
| **Mi perfil** | Alt+P | Mi biografía, mis publicaciones y compartidos, y opciones de la cuenta |

Cada vez que se cambia de pestaña, su contenido se actualiza solo. **Cualquier nombre de usuario**
(en el muro, el buscador, los amigos, las solicitudes o los comentarios) se puede pulsar para
abrir su perfil.

**Aviso al iniciar sesión:** si alguien le envió una solicitud, aparece un aviso por cada una ("Maria
Vargas (@maria) te envió una solicitud de amistad. ¿Quieres aceptarla?") con **Aceptar**,
**Rechazar** y **Decidir después**.

### 3.1 Muro

- Caja **Nueva publicación** ("¿Qué quieres compartir, <nombre>?"), contador **0/500**, selector
  **Visible para: Solo amigos / Pública** (por defecto "Solo amigos") y botón **Publicar** (o
  **Ctrl+Enter**).
- Selector de vista:
  - **Mis amigos**: mis publicaciones y las de mis amigos (públicas y solo amigos) y lo que
    compartimos.
  - **Muro general**: todas las publicaciones **públicas** de la red y sus compartidos.
- Cada tarjeta muestra autor, `@usuario`, fecha, privacidad y "(editada)" si se editó, y los
  botones **Me gusta (N)** (cambia a **Te gusta (N)**; solo se ve el número, nunca quién),
  **Ver comentarios (N)**, **Compartir** (solo en públicas de otros), y en las propias **Editar**
  (solo la más reciente) y **Borrar** (con confirmación).
- Un compartido se ve como "Luis Rojas compartió una publicación de Maria Vargas" con la original
  dentro. Si la original se borró: "La publicación original ya no está disponible".

### 3.2 Anuncios

- Formulario con **Título** (máx. 80) y **texto** (máx. 500) y botón **Publicar anuncio**.
- Lista de anuncios del más reciente al más antiguo; el autor ve el botón **Borrar** en los suyos.

### 3.3 Buscar estudiantes

- **Personas que quizás conozcas**: amigos de mis amigos, con "N amigos en común" y **Enviar
  solicitud** (máximo 10, primero los que tienen más amigos en común).
- Buscador "Nombre, usuario o ID" (sin importar mayúsculas ni tildes; vacío = todos). Cada
  resultado muestra el estado: **Enviar solicitud**, **Solicitud enviada** + **Cancelar**,
  **Responder solicitud**, **Amigos** o **Eres tú**.
- Si dos personas se envían solicitud mutuamente, la amistad se crea sola ("¡Ahora son amigos!").

### 3.4 Mis amigos

- "Tienes N amigos", con `Amigos desde el …` y **Eliminar amigo** (con confirmación; se elimina
  para los dos).

### 3.5 Solicitudes

- **Recibidas**, en el orden en que llegaron (cola): **Aceptar** o **Rechazar**.
- **Enviadas**: con **Cancelar** mientras estén pendientes.

### 3.6 Mi perfil

- Nombre, `@usuario`, ID y biografía (máx. 160).
- Botones: **Editar biografía**, **Editar nombre**, **Cambiar contraseña** (pide la actual, la
  nueva y la confirmación), **Editar última publicación**, **Deshacer última publicación** (solo
  lo publicado desde que se abrió la app) y **Usuarios bloqueados** (con **Desbloquear**).
- Debajo, solo mis publicaciones y mis compartidos.

### 3.7 Perfil de otra persona

- Nombre, `@usuario`, ID, biografía y "N amigos en común".
- Botón de estado de amistad, **Eliminar amigo** (si lo es) y **Bloquear** (con confirmación).
- Sus publicaciones públicas; si son amigos, también las de "Solo amigos".
- **Bloquear** elimina la amistad y las solicitudes, y oculta mutuamente publicaciones,
  comentarios, anuncios, búsqueda, sugerencias y perfiles.

## 4. Cerrar sesión y salir

- **Cerrar sesión** guarda todos los datos y regresa a la pantalla de inicio de sesión.
- Cerrar la ventana con la X también guarda antes de salir. Además, cada acción se guarda en el
  momento en que se hace.
- Toda la información vive en archivos de texto dentro de `data/`.

---

## Guion sugerido para el video (4 minutos)

1. **Intro (10 s):** qué es Social UNA.
2. **Inicio de sesión (15 s):** entrar con `ana` / `ana12345`. Aparece el aviso de la solicitud de
   Maria: pulsar **Decidir después**.
3. **Muro (40 s):** publicar algo **Público**; mostrar "Mis amigos" y "Muro general"; dar
   **Me gusta** y abrir **Ver comentarios** para comentar.
4. **Perfil y deshacer (30 s):** en **Mi perfil**, editar la biografía y pulsar **Deshacer última
   publicación**.
5. **Sugerencias (30 s):** en **Solicitudes**, rechazar la de Maria; en **Buscar estudiantes**,
   Maria aparece en "Personas que quizás conozcas" con 1 amigo en común (Luis).
6. **Perfil de otro y bloqueo (40 s):** pulsar el nombre de Luis para ver su perfil; abrir el de
   Maria y pulsar **Bloquear**: desaparece del muro general, de los anuncios y de las sugerencias.
   Desbloquearla desde **Mi perfil → Usuarios bloqueados**.
7. **Anuncios (20 s):** publicar un anuncio y mostrar que lo ve todo el mundo.
8. **Cierre (15 s):** cerrar sesión y mencionar que los datos se guardan solos.
