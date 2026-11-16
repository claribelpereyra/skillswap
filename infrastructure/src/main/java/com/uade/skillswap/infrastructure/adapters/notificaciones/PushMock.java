package com.uade.skillswap.infrastructure.adapters.notificaciones;

import com.uade.skillswap.domain.notificaciones.CanalPreferido;

public final class PushMock extends CanalMock {
    @Override public CanalPreferido tipo() { return CanalPreferido.PUSH; }
}
