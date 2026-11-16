/*
 * GestorUsuarios.java
 *
 * Qué hace: lógica de negocio de usuarios: cargar y guardar el catálogo, registrar usuarios
 * nuevos, iniciar sesión (con bloqueo por intentos), consultar usuarios y cambiar el nombre o la
 * contraseña de la cuenta.
 * Rol en la arquitectura: capa de SERVICIO. Está en medio de la UI (que le habla por las
 * interfaces ServicioAutenticacion / ServicioRegistro / ServicioCuenta / DirectorioUsuarios) y de la persistencia
 * (a la que le habla por RepositorioUsuarios).
 * Principio SOLID:
 *   - SRP: coordina el caso de uso; delega validación (ValidadorDatosUsuario), criptografía
 *     (HasheadorContrasenas), bloqueo (ControlIntentosLogin), memoria (CatalogoUsuarios),
 *     disco (RepositorioUsuarios) y bitácora (RegistroAuditoria).
 *   - ISP: implementa varias interfaces pequeñas; cada cliente ve solo la que necesita.
 *   - DIP: TODAS sus dependencias llegan por constructor y son abstracciones (no hace "new" de
 *     implementaciones).
 * Colabora con: CatalogoUsuarios, RepositorioUsuarios, HasheadorContrasenas,
 * ControlIntentosLogin, RegistroAuditoria, ValidadorDatosUsuario.
 */
package una.social.servicio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import una.social.auditoria.NivelAuditoria;
import una.social.auditoria.RegistroAuditoria;
import una.social.excepciones.ExcepcionAutenticacion;
import una.social.excepciones.ExcepcionCuentaBloqueada;
import una.social.excepciones.ExcepcionPersistencia;
import una.social.excepciones.ExcepcionUsuarioDuplicado;
import una.social.excepciones.ExcepcionValidacion;
import una.social.modelo.CampoFormulario;
import una.social.modelo.CatalogoUsuarios;
import una.social.modelo.Usuario;
import una.social.persistencia.ModuloPersistente;
import una.social.persistencia.RepositorioUsuarios;
import una.social.seguridad.ControlIntentosLogin;
import una.social.seguridad.CredencialProtegida;
import una.social.seguridad.HasheadorContrasenas;

/**
 * Servicio de usuarios: registro, autenticación, consulta y persistencia del catálogo.
 *
 * <p><b>Hilos:</b> la UI llama a este servicio desde hilos de fondo (SwingWorker) y el cierre
 * de la app lo llama desde otro hilo. Por eso todo acceso al catálogo está sincronizado con
 * {@code synchronized (this)}. El cálculo de PBKDF2 del login se hace FUERA del candado para no
 * frenar otras consultas mientras se verifica una contraseña.</p>
 */
public final class GestorUsuarios implements DirectorioUsuarios, ServicioAutenticacion, ServicioRegistro, ServicioCuenta,
        ModuloPersistente {

    /** Formato de un ID nuevo: prefijo "U" + número con al menos 3 dígitos (U001, U002...). */
    private static final String FORMATO_ID = Usuario.PREFIJO_ID + "%03d";

    /** Largo máximo del nombre escrito que se anota en la auditoría como actor. */
    private static final int LARGO_MAXIMO_ACTOR = 30;

    /** Contraseña de relleno para igualar tiempos cuando el usuario no existe (ver credencialDeRelleno). */
    private static final char[] CONTRASENA_DE_RELLENO = "relleno-no-es-una-cuenta".toCharArray();

    /** Persistencia del catálogo. */
    private final RepositorioUsuarios repositorio;

    /** Estructura de datos en memoria (lista + índices). */
    private final CatalogoUsuarios catalogo;

    /** Protección y verificación de contraseñas. */
    private final HasheadorContrasenas hasheador;

    /** Política de bloqueo por intentos fallidos. */
    private final ControlIntentosLogin controlIntentos;

    /** Bitácora de auditoría. */
    private final RegistroAuditoria auditoria;

    /** Reloj para la fecha de registro (inyectable para pruebas). */
    private final Clock reloj;

    /**
     * Indica si los datos se cargaron bien desde el disco. Si la carga FALLÓ (por ejemplo, el
     * archivo estaba bloqueado), la memoria quedó vacía; guardar en ese estado sobrescribiría el
     * archivo real con una lista vacía y se perderían todos los usuarios. Por eso, mientras sea
     * {@code false}, no se permite guardar.
     */
    private boolean datosCargados;

    /** Credencial ficticia (se calcula una sola vez, la primera vez que hace falta). */
    private CredencialProtegida credencialDeRelleno;

    /**
     * Crea el gestor. No lee el disco todavía: hay que llamar a {@link #cargarDatos()}.
     *
     * @param repositorio     persistencia del catálogo
     * @param catalogo        estructura en memoria (normalmente vacía)
     * @param hasheador       protección de contraseñas
     * @param controlIntentos política de bloqueo
     * @param auditoria       bitácora
     * @param reloj           reloj del sistema
     * @throws NullPointerException si alguna dependencia es nula
     */
    public GestorUsuarios(RepositorioUsuarios repositorio, CatalogoUsuarios catalogo, HasheadorContrasenas hasheador,
                          ControlIntentosLogin controlIntentos, RegistroAuditoria auditoria, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.hasheador = Objects.requireNonNull(hasheador, "hasheador");
        this.controlIntentos = Objects.requireNonNull(controlIntentos, "controlIntentos");
        this.auditoria = Objects.requireNonNull(auditoria, "auditoria");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    // ================================================================== ModuloPersistente

    /** {@inheritDoc} */
    @Override
    public String nombreModulo() {
        return "usuarios";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reconstruye el catálogo en memoria a partir del archivo. Si el archivo trae dos usuarios
     * con el mismo nombre o ID (por edición manual), se conserva el primero y se audita el
     * duplicado.</p>
     */
    @Override
    public synchronized void cargarDatos() {
        // Se espera ExcepcionPersistencia si el archivo no se puede leer. Se marca la carga como
        // fallida (para bloquear guardados que borrarían datos), se audita y se relanza para que
        // Main muestre un aviso al usuario.
        try {
            List<Usuario> leidos = repositorio.cargarTodos();
            catalogo.limpiar();
            for (Usuario usuario : leidos) {
                agregarAlCatalogoSiNoEstaRepetido(usuario);
            }
            datosCargados = true;
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_CARGADOS",
                    "Catálogo de usuarios en memoria: " + catalogo.tamano() + " usuarios");
        } catch (ExcepcionPersistencia ex) {
            datosCargados = false;
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "CARGA_FALLIDA",
                    describirCausa(ex));
            throw ex;
        }
    }

    /**
     * Agrega un usuario leído del archivo, omitiendo duplicados.
     *
     * @param usuario usuario leído
     */
    private void agregarAlCatalogoSiNoEstaRepetido(Usuario usuario) {
        // Se espera ExcepcionUsuarioDuplicado si el archivo fue editado a mano y repite un
        // usuario o un ID. Política: omitir el repetido, auditarlo y seguir.
        try {
            catalogo.agregar(usuario);
        } catch (ExcepcionUsuarioDuplicado ex) {
            auditoria.registrar(NivelAuditoria.WARN, RegistroAuditoria.ACTOR_SISTEMA, "LINEA_CORRUPTA_OMITIDA",
                    "Usuario repetido " + usuario.getId() + " omitido: " + ex.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void guardarDatos() {
        exigirDatosCargados();
        // Se espera ExcepcionPersistencia si el disco falla. Se audita y se relanza para que la
        // UI muestre "no se pudieron guardar los datos".
        try {
            repositorio.guardarTodos(catalogo.listarTodos());
            auditoria.registrar(NivelAuditoria.INFO, RegistroAuditoria.ACTOR_SISTEMA, "DATOS_GUARDADOS",
                    catalogo.tamano() + " usuarios guardados");
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, RegistroAuditoria.ACTOR_SISTEMA, "GUARDADO_FALLIDO", describirCausa(ex));
            throw ex;
        }
    }

    // ================================================================== ServicioRegistro

    /** {@inheritDoc} */
    @Override
    public Usuario registrar(String nombre, String nombreUsuario, char[] contrasena, char[] confirmacion) {
        String actor = actorParaAuditoria(nombreUsuario);
        // Se esperan: ExcepcionValidacion (datos inválidos o usuario repetido) y
        // ExcepcionPersistencia (no se pudo guardar). Ambas se auditan aquí, en el límite del
        // servicio, y se relanzan para que la ventana muestre el mensaje junto al campo o en un
        // diálogo.
        try {
            ValidadorDatosUsuario.validarRegistro(nombre, nombreUsuario, contrasena, confirmacion);
            Usuario creado = crearYGuardar(nombre.trim(), nombreUsuario.trim(), contrasena);
            auditoria.registrar(NivelAuditoria.INFO, actor, "REGISTRO_EXITOSO", "Usuario creado con ID " + creado.getId());
            return creado;
        } catch (ExcepcionValidacion ex) {
            auditoria.registrar(NivelAuditoria.WARN, actor, "REGISTRO_RECHAZADO", ex.getCampo() + ": " + ex.getMessage());
            throw ex;
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, actor, "REGISTRO_FALLIDO", describirCausa(ex));
            throw ex;
        }
    }

    /**
     * Crea el usuario, lo guarda en disco y luego lo agrega a la memoria.
     *
     * <p><b>Orden importante: primero disco, después memoria.</b> Si el guardado falla, la
     * memoria no se tocó y sigue igual al archivo; no hace falta "deshacer" nada.</p>
     *
     * @param nombre        nombre ya validado y sin espacios a los lados
     * @param nombreUsuario usuario ya validado y sin espacios a los lados
     * @param contrasena    contraseña ya validada
     * @return usuario creado
     */
    private synchronized Usuario crearYGuardar(String nombre, String nombreUsuario, char[] contrasena) {
        exigirDatosCargados();
        if (catalogo.contieneNombreUsuario(nombreUsuario)) {
            throw new ExcepcionUsuarioDuplicado("Ese nombre de usuario ya está en uso. Elija otro.");
        }
        CredencialProtegida credencial = hasheador.proteger(contrasena);
        Usuario nuevo = new Usuario(siguienteId(), nombre, nombreUsuario,
                credencial.hashBase64(), credencial.salBase64(), Instant.now(reloj));
        List<Usuario> conNuevo = new ArrayList<>(catalogo.listarTodos());
        conNuevo.add(nuevo);
        repositorio.guardarTodos(conNuevo);
        catalogo.agregar(nuevo);
        return nuevo;
    }

    /**
     * Calcula el siguiente ID: el número más alto existente + 1, con al menos 3 dígitos.
     * Se usa el MÁXIMO (y no la cantidad de usuarios) para que nunca se repita un ID aunque se
     * hayan omitido líneas corruptas o haya huecos en la numeración.
     *
     * @return nuevo ID único (por ejemplo "U004")
     */
    private String siguienteId() {
        // Invariante: solo se llama con el candado tomado (desde crearYGuardar), porque dos
        // registros simultáneos podrían obtener el mismo número.
        assert Thread.holdsLock(this) : "siguienteId debe llamarse con el candado del gestor";
        int maximo = 0;
        for (Usuario usuario : catalogo.listarTodos()) {
            Matcher m = Usuario.PATRON_ID.matcher(usuario.getId());
            // Invariante: todo ID en el catálogo ya fue validado con PATRON_ID al leer el archivo
            // o al crearse aquí; si no coincide, hay un error de programación.
            assert m.matches() : "ID inválido dentro del catálogo: " + usuario.getId();
            maximo = Math.max(maximo, Integer.parseInt(usuario.getId().substring(Usuario.PREFIJO_ID.length())));
        }
        return String.format(FORMATO_ID, maximo + 1);
    }

    // ================================================================== ServicioCuenta

    /** {@inheritDoc} */
    @Override
    public Usuario cambiarNombre(Usuario usuario, String nombreNuevo) {
        Objects.requireNonNull(usuario, "usuario");
        String actor = usuario.getNombreUsuario();
        // Se esperan ExcepcionValidacion (nombre inválido) y ExcepcionPersistencia (no se pudo
        // guardar). Se auditan y se relanzan para que el perfil muestre el mensaje.
        try {
            ValidadorDatosUsuario.validarCambioNombre(nombreNuevo);
            Usuario actualizado = reemplazarYGuardar(usuario.getId(),
                    actual -> new Usuario(actual.getId(), nombreNuevo.trim(), actual.getNombreUsuario(),
                            actual.getHashContrasena(), actual.getSal(), actual.getFechaRegistro()));
            auditoria.registrar(NivelAuditoria.INFO, actor, "NOMBRE_CAMBIADO", "Nombre de " + usuario.getId() + " actualizado");
            return actualizado;
        } catch (ExcepcionValidacion ex) {
            auditoria.registrar(NivelAuditoria.WARN, actor, "CAMBIO_NOMBRE_RECHAZADO", ex.getCampo() + ": " + ex.getMessage());
            throw ex;
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, actor, "CAMBIO_NOMBRE_FALLIDO", describirCausa(ex));
            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>La contraseña actual se verifica con el MISMO hasheador que el login (comparación en
     * tiempo constante). La nueva se protege con {@code hasheador.proteger}, que con PBKDF2 genera
     * una sal NUEVA. En la auditoría nunca se escribe ninguna contraseña.</p>
     */
    @Override
    public void cambiarContrasena(Usuario usuario, char[] actual, char[] nueva, char[] confirmacion) {
        Objects.requireNonNull(usuario, "usuario");
        String actor = usuario.getNombreUsuario();
        // Se esperan ExcepcionValidacion (actual incorrecta o nueva inválida) y
        // ExcepcionPersistencia (no se pudo guardar). Se auditan sin datos sensibles y se relanzan.
        try {
            ValidadorDatosUsuario.validarCambioContrasena(actual, nueva, confirmacion);
            reemplazarYGuardar(usuario.getId(), guardado -> {
                if (!hasheador.verificar(actual, new CredencialProtegida(guardado.getSal(), guardado.getHashContrasena()))) {
                    throw new ExcepcionValidacion(CampoFormulario.CONTRASENA_ACTUAL, "La contraseña actual no es correcta.");
                }
                CredencialProtegida nuevaCredencial = hasheador.proteger(nueva);
                return new Usuario(guardado.getId(), guardado.getNombre(), guardado.getNombreUsuario(),
                        nuevaCredencial.hashBase64(), nuevaCredencial.salBase64(), guardado.getFechaRegistro());
            });
            auditoria.registrar(NivelAuditoria.INFO, actor, "CONTRASENA_CAMBIADA", "Contraseña de " + usuario.getId() + " actualizada");
        } catch (ExcepcionValidacion ex) {
            auditoria.registrar(NivelAuditoria.WARN, actor, "CAMBIO_CONTRASENA_RECHAZADO", ex.getCampo() + ": " + ex.getMessage());
            throw ex;
        } catch (ExcepcionPersistencia ex) {
            auditoria.registrar(NivelAuditoria.ERROR, actor, "CAMBIO_CONTRASENA_FALLIDO", describirCausa(ex));
            throw ex;
        }
    }

    /**
     * Reemplaza un usuario por su versión actualizada: primero en disco, después en el catálogo.
     *
     * @param idUsuario usuario a actualizar
     * @param cambio    función que recibe el usuario guardado y devuelve el actualizado (puede lanzar
     *                  ExcepcionValidacion, por ejemplo si la contraseña actual es incorrecta)
     * @return el usuario actualizado
     */
    private synchronized Usuario reemplazarYGuardar(String idUsuario, UnaryOperator<Usuario> cambio) {
        exigirDatosCargados();
        Usuario guardado = catalogo.buscarPorId(idUsuario)
                .orElseThrow(() -> new ExcepcionPersistencia("El usuario ya no existe en el catálogo."));
        Usuario actualizado = cambio.apply(guardado);
        // Invariante: un cambio de cuenta nunca altera la identidad (ID ni nombre de usuario).
        assert actualizado.getId().equals(guardado.getId())
                && actualizado.getNombreUsuario().equals(guardado.getNombreUsuario()) : "El cambio alteró la identidad";
        List<Usuario> nuevos = new ArrayList<>(catalogo.listarTodos());
        nuevos.set(nuevos.indexOf(guardado), actualizado);
        repositorio.guardarTodos(nuevos);
        catalogo.reemplazar(actualizado);
        return actualizado;
    }

    // ================================================================== ServicioAutenticacion

    /** {@inheritDoc} */
    @Override
    public Usuario autenticar(String nombreUsuario, char[] contrasena) {
        ValidadorDatosUsuario.validarLogin(nombreUsuario, contrasena);
        String clave = CatalogoUsuarios.normalizarClave(nombreUsuario);
        String actor = actorParaAuditoria(nombreUsuario);
        rechazarSiEstaBloqueado(clave, actor);
        Optional<Usuario> encontrado = buscarPorNombreUsuario(nombreUsuario);
        if (!credencialesValidas(encontrado, contrasena)) {
            registrarFalloYLanzar(clave, actor);
        }
        controlIntentos.reiniciar(clave);
        Usuario usuario = encontrado.orElseThrow();
        auditoria.registrar(NivelAuditoria.INFO, usuario.getNombreUsuario(), "LOGIN_EXITOSO", "Sesión iniciada (" + usuario.getId() + ")");
        return usuario;
    }

    /**
     * Si la clave está bloqueada, audita el intento y lanza la excepción de bloqueo.
     *
     * @param clave nombre normalizado
     * @param actor nombre para la auditoría
     * @throws ExcepcionCuentaBloqueada si está bloqueada
     */
    private void rechazarSiEstaBloqueado(String clave, String actor) {
        Optional<Duration> restante = controlIntentos.tiempoBloqueoRestante(clave);
        if (restante.isPresent()) {
            auditoria.registrar(NivelAuditoria.WARN, actor, "LOGIN_BLOQUEADO", "Intento durante un bloqueo temporal");
            throw new ExcepcionCuentaBloqueada(restante.get());
        }
    }

    /**
     * Verifica la contraseña. Si el usuario NO existe, igual se calcula un PBKDF2 contra una
     * credencial de relleno: así el login tarda lo mismo exista o no el usuario, y un atacante
     * no puede averiguar qué usuarios existen midiendo el tiempo de respuesta.
     *
     * @param encontrado usuario buscado (puede estar vacío)
     * @param contrasena contraseña escrita
     * @return {@code true} si el usuario existe y la contraseña es correcta
     */
    private boolean credencialesValidas(Optional<Usuario> encontrado, char[] contrasena) {
        if (encontrado.isEmpty()) {
            hasheador.verificar(contrasena, credencialDeRelleno());
            return false;
        }
        Usuario usuario = encontrado.get();
        return hasheador.verificar(contrasena, new CredencialProtegida(usuario.getSal(), usuario.getHashContrasena()));
    }

    /**
     * Anota el fallo, lo audita y lanza la excepción que corresponda.
     *
     * @param clave nombre normalizado
     * @param actor nombre para la auditoría
     * @throws ExcepcionCuentaBloqueada si este fallo provocó el bloqueo
     * @throws ExcepcionAutenticacion   en cualquier otro caso (mensaje genérico)
     */
    private void registrarFalloYLanzar(String clave, String actor) {
        boolean seBloqueo = controlIntentos.registrarFallo(clave);
        auditoria.registrar(NivelAuditoria.WARN, actor, "LOGIN_FALLIDO", "Intento fallido "
                + controlIntentos.intentosFallidos(clave) + " de " + controlIntentos.getMaximoIntentos());
        if (seBloqueo) {
            auditoria.registrar(NivelAuditoria.WARN, actor, "CUENTA_BLOQUEADA", "Bloqueo temporal de "
                    + controlIntentos.getDuracionBloqueo().toMinutes() + " minutos por intentos fallidos");
            throw new ExcepcionCuentaBloqueada(controlIntentos.getDuracionBloqueo());
        }
        throw new ExcepcionAutenticacion();
    }

    /**
     * Devuelve (calculándola la primera vez) una credencial ficticia para igualar tiempos.
     *
     * @return credencial de relleno
     */
    private synchronized CredencialProtegida credencialDeRelleno() {
        if (credencialDeRelleno == null) {
            credencialDeRelleno = hasheador.proteger(CONTRASENA_DE_RELLENO);
        }
        return credencialDeRelleno;
    }

    // ================================================================== DirectorioUsuarios

    /** {@inheritDoc} */
    @Override
    public synchronized Optional<Usuario> buscarPorId(String id) {
        return catalogo.buscarPorId(id);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        return catalogo.buscarPorNombreUsuario(nombreUsuario);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<Usuario> listarTodos() {
        return catalogo.listarTodos();
    }

    // ================================================================== utilidades

    /**
     * Impide guardar si la carga inicial falló (ver {@link #datosCargados}).
     *
     * @throws ExcepcionPersistencia si los datos no se cargaron correctamente
     */
    private void exigirDatosCargados() {
        if (!datosCargados) {
            throw new ExcepcionPersistencia("Los datos de usuarios no se cargaron correctamente; "
                    + "no se guardará nada para no sobrescribir el archivo. Reinicie la aplicación.");
        }
    }

    /**
     * Prepara el nombre escrito para usarlo como actor en la auditoría: sin espacios a los lados
     * y recortado (por si alguien pegó un texto enorme).
     *
     * @param nombreUsuario nombre escrito
     * @return actor seguro
     */
    private static String actorParaAuditoria(String nombreUsuario) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            return "(vacío)";
        }
        String limpio = nombreUsuario.trim();
        return limpio.length() <= LARGO_MAXIMO_ACTOR ? limpio : limpio.substring(0, LARGO_MAXIMO_ACTOR);
    }

    /**
     * Describe una excepción y su causa técnica para la auditoría.
     *
     * @param ex excepción
     * @return texto con el mensaje amigable y la causa técnica
     */
    private static String describirCausa(RuntimeException ex) {
        String texto = ex.getMessage();
        return ex.getCause() == null ? texto : texto + " Causa: " + RegistroAuditoria.describirError(ex.getCause());
    }
}
