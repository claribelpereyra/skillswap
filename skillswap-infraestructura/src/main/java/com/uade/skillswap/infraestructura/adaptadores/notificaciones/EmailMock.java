package com.uade.skillswap.infraestructura.adaptadores.notificaciones;

import com.uade.skillswap.dominio.notificaciones.CanalPreferido;

public final class EmailMock extends CanalMock {
    @Override public CanalPreferido tipo() { return CanalPreferido.EMAIL; }
}
