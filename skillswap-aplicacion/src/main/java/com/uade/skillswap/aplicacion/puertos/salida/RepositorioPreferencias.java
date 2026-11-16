package com.uade.skillswap.aplicacion.puertos.salida;

import com.uade.skillswap.dominio.notificaciones.PreferenciaNotificacion;
import java.util.Optional;

public interface RepositorioPreferencias {
    Optional<PreferenciaNotificacion> buscar(String userId);
    void guardar(PreferenciaNotificacion preferencia);
}
