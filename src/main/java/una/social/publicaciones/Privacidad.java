/*
 * Privacidad.java
 *
 * Qué hace: indica quién puede ver una publicación: todos ("Pública") o solo los amigos del autor
 * ("Solo amigos").
 * Rol en la arquitectura: parte del modelo de publicaciones; se guarda como una columna de
 * publicaciones.txt y decide en qué muro aparece cada publicación.
 * Principio SOLID: SRP (solo clasifica la visibilidad) y OCP (un nivel nuevo se agrega aquí).
 * Colabora con: Publicacion, GestorPublicaciones, PanelMuro.
 */
package una.social.publicaciones;

/**
 * Nivel de privacidad de una publicación.
 */
public enum Privacidad {

    /** Visible para todos en el "Muro general" (y compartible). */
    PUBLICA("Pública"),

    /** Visible solo para el autor y sus amigos (vista "Mis amigos"). Es el valor por defecto. */
    SOLO_AMIGOS("Solo amigos");

    /** Texto para mostrar en la interfaz. */
    private final String etiqueta;

    /**
     * @param etiqueta texto para mostrar
     */
    Privacidad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * @return texto para mostrar en la interfaz ("Pública" o "Solo amigos")
     */
    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * Se sobrescribe para que los combos de la UI muestren la etiqueta en español.
     *
     * @return la etiqueta
     */
    @Override
    public String toString() {
        return etiqueta;
    }
}
