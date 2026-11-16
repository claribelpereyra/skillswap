package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.model.Models.*;
import java.util.List;

public interface UiControllers {
    interface BusquedaController {
        List<Publicacion> buscar(FiltrosBusqueda filtros);
        List<Publicacion> recomendar(String userId);
        List<String> categorias();
    }

    interface PublicacionController {
        List<Publicacion> listarPropias(String userId);
        Publicacion crearBorrador(String userId, String titulo, String descripcion,
                                  String categoria, Modalidad modalidad, int duracion, int precio);
        Publicacion cambiarEstado(String userId, String publicacionId, EstadoPublicacion estado);
        Publicacion cambiarEstado(String userId, String publicacionId, EstadoPublicacion estado, String motivo);
        List<Franja> franjas(String userId, String publicacionId);
        Publicacion crearBorrador(String userId, String titulo, String descripcion, String categoria,
                Modalidad modalidad, int duracion, int precio, String tipo, List<Franja> franjas);
    }

    interface ContratacionController {
        List<Contratacion> listarContrataciones(String userId);
        Contratacion solicitar(String userId, String publicacionId, String objetivo);
        Contratacion avanzar(String userId, String contratacionId);
        Contratacion cancelar(String userId, String contratacionId);
        Contratacion cancelar(String userId, String id, String motivo);
        void rechazar(String userId, String id, String motivo);
        Contratacion solicitar(String userId, String publicacionId, String franjaId, String mensaje, String objetivo);
        List<Franja> franjasParaContrato(String userId, String id);
        void reprogramar(String userId, String id, String franjaId);
        boolean tieneReprogramacion(String userId, String id);
        void responderReprogramacion(String userId, String id, boolean aceptar);
        void calificar(String userId, String id, int puntaje, String comentario);
        String comprobante(String userId, String id);
    }

    interface WalletController {
        int consultarSaldo(String userId);
        List<Movimiento> listarMovimientos(String userId);
        int acreditarDemo(String userId, int importe);
    }

    interface NotificacionController {
        List<Notificacion> listarNotificaciones(String userId);
        void marcarLeida(String userId, String notificacionId);
        void marcarTodasLeidas(String userId);
        String canal(String userId);
        void cambiarCanal(String userId, String canal);
    }

    interface PerfilController {
        Perfil verPerfil(String userId);
    }

    BusquedaController busqueda();
    PublicacionController publicaciones();
    ContratacionController contrataciones();
    WalletController wallet();
    NotificacionController notificaciones();
    PerfilController perfiles();
    int pagina(String listado);
    boolean haySiguiente(String listado);
    void cambiarPagina(String listado, int pagina);
}
