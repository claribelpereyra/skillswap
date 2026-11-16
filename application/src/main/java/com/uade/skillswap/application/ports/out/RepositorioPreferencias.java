package com.uade.skillswap.application.ports.out;

import com.uade.skillswap.domain.notificaciones.PreferenciaNotificacion;
import java.util.Optional;

public interface RepositorioPreferencias {
    Optional<PreferenciaNotificacion> buscar(String userId);
    void guardar(PreferenciaNotificacion preferencia);
}
