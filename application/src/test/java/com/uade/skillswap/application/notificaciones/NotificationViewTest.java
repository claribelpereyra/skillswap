package com.uade.skillswap.application.notificaciones;

import com.uade.skillswap.application.dto.notification.NotificationView;
import com.uade.skillswap.domain.notificaciones.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class NotificationViewTest {
    @Test void exposesTheInformationNeededByTheInboxIncludingReadAndDeliveryStates() {
        var id = UUID.randomUUID();
        var aviso = new Aviso(UUID.randomUUID(), Instant.EPOCH, TipoEventoNotificable.ACEPTACION,
                "booking-1", "ana", "Solicitud aceptada");
        var unread = NotificationView.from(new Notificacion(id, aviso, CanalPreferido.PUSH, EstadoEntrega.FALLIDA, null));
        assertThat(unread.id()).isEqualTo(id);
        assertThat(unread.type()).isEqualTo("ACEPTACION");
        assertThat(unread.reference()).isEqualTo("booking-1");
        assertThat(unread.message()).isEqualTo("Solicitud aceptada");
        assertThat(unread.channel()).isEqualTo("PUSH");
        assertThat(unread.delivery()).isEqualTo("FALLIDA");
        assertThat(unread.occurredAt()).isEqualTo(Instant.EPOCH);
        assertThat(unread.read()).isFalse();
        assertThat(unread.readAt()).isNull();
        var read = NotificationView.from(new Notificacion(id, aviso, CanalPreferido.PUSH,
                EstadoEntrega.ENVIADA, Instant.EPOCH.plusSeconds(60)));
        assertThat(read.read()).isTrue();
        assertThat(read.readAt()).isEqualTo(Instant.EPOCH.plusSeconds(60));
        assertThat(read.delivery()).isEqualTo("ENVIADA");
    }
}
