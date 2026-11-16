package com.uade.skillswap.aplicacion.puertos.salida;

import com.uade.skillswap.dominio.notificaciones.*;

public interface CanalNotificacion {
    CanalPreferido tipo();
    /** Envía o informa un fallo mediante excepción; no confirma entrega de un proveedor real. */
    void enviar(Aviso aviso);
}
