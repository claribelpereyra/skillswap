package com.uade.skillswap.controllers.notification;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.ports.in.PreferenciaNotificacionEntrada;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.domain.notificaciones.CanalPreferido;

import java.util.Objects;
import static com.uade.skillswap.controllers.common.ControllerSupport.requireEnum;

public final class NotificationPreferencesControllerImpl implements NotificationPreferencesController {
    private final PreferenciaNotificacionEntrada preferences;
    private final IdentityService identity;

    public NotificationPreferencesControllerImpl(PreferenciaNotificacionEntrada preferences, IdentityService identity) {
        this.preferences = Objects.requireNonNull(preferences);
        this.identity = Objects.requireNonNull(identity);
    }

    @Override
    public ControllerResult<String> getChannel() {
        return ControllerResult.of(() -> preferences.consultar(actor()).channel().name());
    }

    @Override
    public ControllerResult<String> changeChannel(String channel) {
        return ControllerResult.of(() -> {
            String actor = actor();
            CanalPreferido selected = requireEnum(CanalPreferido.class, channel, "channel");
            preferences.cambiar(actor, selected);
            return selected.name();
        });
    }

    private String actor() {
        var actor = identity.currentUserId();
        if (actor == null) throw new AccessDeniedException("No active user");
        return actor.value();
    }
}
