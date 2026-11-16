# Contexto para el video explicativo de Social UNA

Este documento describe la interfaz de Social UNA tal como funciona hoy, pantalla por pantalla,
para preparar un video corto que se ofrecerá al iniciar sesión (opcional para el usuario).

## Qué es Social UNA (para la introducción del video)

Red social estudiantil de la Universidad Nacional (Costa Rica). Cada estudiante crea su cuenta,
inicia sesión, busca a otros estudiantes, les envía solicitudes de amistad (que la otra persona
acepta o rechaza) y publica mensajes en un muro.
En el muro ve sus publicaciones y las de sus amigos, de la más reciente a la más antigua.

## Usuarios para grabar la demostración

| ID | Nombre | Usuario | Contraseña |
|----|--------|---------|------------|
| U001 | Ana Mora | `ana` | `ana12345` |
| U002 | Luis Rojas | `luis` | `luis12345` |
| U003 | Maria Vargas | `maria` | `maria12345` |

Amistades iniciales: **Ana ↔ Luis** y **Luis ↔ Maria**. Ana y Maria **no** son amigas, así que
"Ana le envía una solicitud a Maria" es el ejemplo ideal para mostrar cómo funcionan las amistades.
Hay 6 publicaciones de ejemplo (2 por usuario).

Para empezar la grabación con los datos limpios, se puede regenerar con
`java -cp target\social-una.jar una.social.herramientas.HerramientaDatosSemilla --reiniciar`.

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
- Cuatro pestañas, cada una con atajo de teclado:

| Pestaña | Atajo | Para qué sirve |
|---------|-------|----------------|
| **Muro** | Alt+M | Ver y escribir publicaciones |
| **Buscar estudiantes** | Alt+B | Encontrar estudiantes y enviarles solicitudes de amistad |
| **Mis amigos** | Alt+A | Ver la lista de amigos y eliminar amigos |
| **Solicitudes** | Alt+S | Aceptar o rechazar las solicitudes recibidas; muestra cuántas hay, ej. "Solicitudes (1)" |

Cada vez que se cambia de pestaña, su contenido se actualiza solo.

**Aviso al iniciar sesión:** si alguien le envió una solicitud de amistad, al abrirse la ventana
aparece un aviso por cada una: "Ana Mora (@ana) te envió una solicitud de amistad. ¿Quieres
aceptarla?" con los botones **Aceptar**, **Rechazar** y **Decidir después** (la deja pendiente en
la pestaña Solicitudes).

### 3.1 Muro

- Caja **Nueva publicación** con el texto de ayuda "¿Qué quieres compartir, <nombre>?".
- Contador de caracteres **0/500**. Se pone en rojo si se pasa del máximo.
- Botón **Publicar**. También se puede publicar con **Ctrl+Enter** (Enter solo hace salto de línea).
- Botón **Actualizar muro**.
- Debajo, las publicaciones de **uno mismo y de sus amigos**, de la más reciente a la más antigua,
  con autor, fecha y hora (dd/MM/yyyy HH:mm). Las propias aparecen marcadas con "(tú)".
- Si el muro está vacío, la pantalla invita a escribir la primera publicación o a agregar amigos
  en "Buscar estudiantes".
- No se puede publicar un texto vacío ni de más de 500 caracteres.

### 3.2 Buscar estudiantes

- Campo de búsqueda "Nombre, usuario o ID (ej. luis, Vargas, U003)" y botón **Buscar**.
- No importan las mayúsculas ni las tildes.
- Si se busca con el campo vacío, aparecen **todos** los estudiantes registrados.
- Cada resultado muestra avatar, nombre, `@usuario · ID` y, a la derecha, según el caso:
  - **Enviar solicitud** → aviso "Solicitud enviada": la amistad NO se crea todavía; la otra
    persona decide si la acepta.
  - **Solicitud enviada** + botón **Cancelar solicitud** (pide confirmación), si ya se la envió.
  - **Responder solicitud**, si esa persona le envió una a uno: abre el aviso con Aceptar /
    Rechazar / Decidir después.
  - **Ya son amigos** (texto).
  - **Eres tú** (en la fila propia).
- La amistad es **mutua**: cuando Maria acepta la solicitud de Ana, las dos se ven como amigas.

### 3.3 Mis amigos

- Resumen: "Tienes N amigos" (o "Tienes 1 amigo").
- Lista con avatar, nombre, `@usuario · ID · Amigos desde el …` y el botón **Eliminar amigo**.
- **Eliminar amigo** pide confirmación ("¿Seguro que quieres eliminar a … de tus amigos? Ya no
  verán las publicaciones del otro en el muro.") y elimina la amistad **para los dos**. Después se
  le puede volver a enviar una solicitud.
- Si no tiene amigos, indica que vaya a "Buscar estudiantes" y pulse "Enviar solicitud".

### 3.4 Solicitudes

- Resumen: "Tienes N solicitudes de amistad".
- Cada solicitud muestra quién la envió (`@usuario · ID`), la fecha y hora, y los botones
  **Aceptar** (aparece "¡Nuevo amigo!") y **Rechazar** (pide confirmación; a la otra persona no se
  le avisa).
- Si no hay, dice "No tienes solicitudes de amistad pendientes".

## 4. Cerrar sesión y salir

- **Cerrar sesión** guarda todos los datos y regresa a la pantalla de inicio de sesión.
- Cerrar la ventana con la X también guarda antes de salir.
- Toda la información se guarda en archivos de texto dentro de `data/` (usuarios, amistades,
  solicitudes pendientes y publicaciones). Al volver a abrir la aplicación, todo sigue ahí.

---

## Guion sugerido para el video (3 minutos)

1. **Intro (10 s):** qué es Social UNA.
2. **Registro (30 s):** crear una cuenta nueva, mostrar el ID asignado y un error de validación
   (por ejemplo, contraseñas que no coinciden).
3. **Inicio de sesión (15 s):** entrar con `ana` / `ana12345` y mencionar "Mostrar contraseña".
4. **Muro (25 s):** escribir y publicar (con Ctrl+Enter) y señalar que se ven las publicaciones de
   Ana y de Luis (su amigo), pero no las de Maria.
5. **Enviar solicitud (20 s):** en "Buscar estudiantes", buscar "maria" y pulsar **Enviar
   solicitud**. El botón cambia a "Solicitud enviada / Cancelar solicitud".
6. **Aceptar la solicitud (30 s):** cerrar sesión y entrar como `maria` / `maria12345`. Aparece
   el aviso "Ana Mora te envió una solicitud de amistad"; pulsar **Aceptar** → "¡Nuevo amigo!".
   (Mencionar que también se puede responder desde la pestaña **Solicitudes**.)
7. **Muro de Maria (15 s):** ahora aparecen también las publicaciones de Ana.
8. **Mis amigos y eliminar (25 s):** mostrar a Ana y Luis con la fecha de amistad; pulsar
   **Eliminar amigo** en uno de ellos para enseñar la confirmación (se puede cancelar).
9. **Cierre (10 s):** cerrar sesión, recordar que los datos se guardan solos y mencionar los
   atajos Alt+M, Alt+B, Alt+A y Alt+S.
