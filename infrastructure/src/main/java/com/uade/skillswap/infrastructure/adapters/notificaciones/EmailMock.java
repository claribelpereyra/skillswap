package com.uade.skillswap.infrastructure.adapters.notificaciones;

import com.uade.skillswap.domain.notificaciones.CanalPreferido;

public final class EmailMock extends CanalMock {
    @Override public CanalPreferido tipo() { return CanalPreferido.EMAIL; }
}
