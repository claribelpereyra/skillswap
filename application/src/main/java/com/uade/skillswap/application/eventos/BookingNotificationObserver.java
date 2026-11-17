package com.uade.skillswap.application.eventos;

import com.uade.skillswap.application.ports.in.ProcesarEventoNotificableEntrada;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.booking.observer.BookingEvent;
import com.uade.skillswap.domain.booking.observer.BookingObserver;
import com.uade.skillswap.domain.notificaciones.EventoNotificable;
import com.uade.skillswap.domain.notificaciones.TipoEventoNotificable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Adapta transiciones confirmadas a los seis eventos del contrato de notificaciones. */
public final class BookingNotificationObserver implements BookingObserver {
    private final ProcesarEventoNotificableEntrada processor;

    public BookingNotificationObserver(ProcesarEventoNotificableEntrada processor) {
        this.processor = Objects.requireNonNull(processor);
    }

    @Override
    public void update(BookingEvent event) {
        TipoEventoNotificable type = typeOf(event);
        if (type == null) return;
        String reference = "booking-" + event.bookingId();
        // Una retransmisión de la misma transición conserva su id y no duplica avisos.
        String key = reference + "|" + event.from() + "|" + event.to() + "|" + event.actor().value()
                + "|" + event.occurredAt() + "|" + event.reason();
        UUID id = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        processor.procesar(new EventoNotificable(id, event.occurredAt(), type, reference,
                List.of(event.clientId().value(), event.providerId().value()),
                type.name() + ": " + reference + (event.reason() == null ? "" : " - " + event.reason())));
    }

    private static TipoEventoNotificable typeOf(BookingEvent event) {
        if (event.from() == null && event.to() == BookingStatus.PENDING)
            return TipoEventoNotificable.SOLICITUD_RECIBIDA;
        if (event.from() == event.to() && event.reason() != null
                && event.reason().startsWith("Rescheduled to slot "))
            return TipoEventoNotificable.REPROGRAMACION;
        if (event.from() == event.to()) return null;
        return switch (event.to()) {
            case ACCEPTED -> TipoEventoNotificable.ACEPTACION;
            case CONFIRMED -> TipoEventoNotificable.PAGO_CONFIRMADO;
            case CANCELLED -> TipoEventoNotificable.CANCELACION;
            case COMPLETED -> TipoEventoNotificable.SESION_COMPLETADA;
            default -> null;
        };
    }
}
