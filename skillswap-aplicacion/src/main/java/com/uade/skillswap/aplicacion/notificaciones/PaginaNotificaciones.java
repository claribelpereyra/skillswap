package com.uade.skillswap.aplicacion.notificaciones;

import com.uade.skillswap.dominio.notificaciones.Notificacion;
import java.util.List;

/** DTO local hasta integrar el contrato Pagina común del equipo. */
public record PaginaNotificaciones(List<Notificacion> elementos, int pagina, int tamanio, long total) {
    public PaginaNotificaciones {
        validar(pagina, tamanio);
        elementos = List.copyOf(elementos);
        if (total < 0 || elementos.size() > tamanio || elementos.size() > total) {
            throw new IllegalArgumentException("Página inconsistente");
        }
    }

    public static void validar(int pagina, int tamanio) {
        if (pagina < 0 || tamanio < 1 || tamanio > 100 || (long) pagina * tamanio > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Página inválida: tamaño entre 1 y 100 y desplazamiento válido");
        }
    }
}
