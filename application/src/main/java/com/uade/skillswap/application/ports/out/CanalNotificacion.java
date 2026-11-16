package com.uade.skillswap.application.ports.out;

import com.uade.skillswap.domain.notificaciones.*;

public interface CanalNotificacion {
    CanalPreferido tipo();
    /** Envía o informa un fallo mediante excepción; no confirma entrega de un proveedor real. */
    void enviar(Aviso aviso);
}
