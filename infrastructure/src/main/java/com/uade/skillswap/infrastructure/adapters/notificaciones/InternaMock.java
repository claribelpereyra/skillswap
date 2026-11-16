package com.uade.skillswap.infrastructure.adapters.notificaciones;

import com.uade.skillswap.domain.notificaciones.CanalPreferido;

public final class InternaMock extends CanalMock {
    @Override public CanalPreferido tipo() { return CanalPreferido.INTERNA; }
}
