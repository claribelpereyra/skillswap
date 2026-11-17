package com.uade.skillswap.domain.notificaciones;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificacionesDominioTest {

    private static final UUID ID = UUID.randomUUID();
    private static final Instant AT = Instant.parse("2026-11-16T10:00:00Z");
    private static final TipoEventoNotificable TIPO = TipoEventoNotificable.PAGO_CONFIRMADO;

    private static Aviso aviso() {
        return new Aviso(ID, AT, TIPO, "booking-1", "ana", "PAGO_CONFIRMADO: booking-1");
    }

    @Test
    void anEventNeedsReferenceDetailAndDistinctRecipients() {
        var evento = new EventoNotificable(ID, AT, TIPO, "booking-1", List.of("ana", "bruno"), "detalle");
        assertThat(evento.destinatarios()).containsExactly("ana", "bruno");

        assertThatThrownBy(() -> new EventoNotificable(ID, AT, TIPO, " ", List.of("ana"), "detalle"))
                .hasMessage("Falta referencia");
        assertThatThrownBy(() -> new EventoNotificable(ID, AT, TIPO, "booking-1", List.of("ana"), null))
                .hasMessage("Falta detalle");
        assertThatThrownBy(() -> new EventoNotificable(ID, AT, TIPO, "booking-1", List.of(), "detalle"))
                .hasMessage("Faltan destinatarios válidos");
        assertThatThrownBy(() -> new EventoNotificable(ID, AT, TIPO, "booking-1", List.of("ana", " "), "detalle"))
                .hasMessage("Faltan destinatarios válidos");
        assertThatThrownBy(() -> new EventoNotificable(ID, AT, TIPO, "booking-1", List.of("ana", "ana"), "detalle"))
                .hasMessage("Destinatarios repetidos");
        assertThatThrownBy(() -> new EventoNotificable(null, AT, TIPO, "booking-1", List.of("ana"), "detalle"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aNoticeIsAddressedToOneCompleteRecipient() {
        assertThat(aviso().destinatario()).isEqualTo("ana");
        assertThatThrownBy(() -> new Aviso(ID, AT, TIPO, "", "ana", "detalle"))
                .hasMessage("Aviso incompleto");
        assertThatThrownBy(() -> new Aviso(ID, AT, TIPO, "booking-1", null, "detalle"))
                .hasMessage("Aviso incompleto");
        assertThatThrownBy(() -> new Aviso(ID, AT, TIPO, "booking-1", "ana", " "))
                .hasMessage("Aviso incompleto");
        assertThatThrownBy(() -> new Aviso(ID, null, TIPO, "booking-1", "ana", "detalle"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aNotificationIsReadOnlyWhenItHasAReadDate() {
        var pendiente = new Notificacion(ID, aviso(), CanalPreferido.EMAIL, EstadoEntrega.PENDIENTE, null);
        var leida = new Notificacion(ID, aviso(), CanalPreferido.EMAIL, EstadoEntrega.ENVIADA, AT);

        assertThat(pendiente.leida()).isFalse();
        assertThat(leida.leida()).isTrue();
        assertThatThrownBy(() -> new Notificacion(ID, aviso(), null, EstadoEntrega.ENVIADA, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aPreferenceNeedsAUserAndAChannel() {
        assertThat(new PreferenciaNotificacion("ana", CanalPreferido.PUSH).canal()).isEqualTo(CanalPreferido.PUSH);
        assertThatThrownBy(() -> new PreferenciaNotificacion(" ", CanalPreferido.PUSH)).hasMessage("Falta userId");
        assertThatThrownBy(() -> new PreferenciaNotificacion("ana", null)).hasMessage("Falta canal");
    }

    @Test
    void theSixNotifiableEventsOfTheAssignmentExist() {
        assertThat(TipoEventoNotificable.values()).containsExactly(TipoEventoNotificable.SOLICITUD_RECIBIDA,
                TipoEventoNotificable.ACEPTACION, TipoEventoNotificable.PAGO_CONFIRMADO,
                TipoEventoNotificable.REPROGRAMACION, TipoEventoNotificable.CANCELACION,
                TipoEventoNotificable.SESION_COMPLETADA);
    }
}
