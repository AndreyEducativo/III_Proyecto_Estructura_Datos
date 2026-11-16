/*
 * SeccionPrincipal.java
 *
 * Qué hace: contrato de cada sección (pestaña) de la ventana principal: muro, buscador y amigos.
 * Rol en la arquitectura: la VentanaPrincipal llama a refrescar() cada vez que el usuario abre una
 * pestaña, así siempre ve datos actualizados (por ejemplo, las publicaciones de un amigo recién
 * agregado).
 * Principio SOLID: ISP (un solo método) y OCP (una sección nueva solo implementa esta interfaz).
 * Colabora con: VentanaPrincipal, PanelMuro, PanelBuscador, PanelAmigos.
 */
package una.social.ui;

/**
 * Sección de la ventana principal que sabe actualizar su contenido.
 */
interface SeccionPrincipal {

    /**
     * Vuelve a consultar los datos y redibuja la sección. Se llama desde el hilo de Swing; la
     * consulta se hace en segundo plano para no congelar la ventana.
     */
    void refrescar();
}
