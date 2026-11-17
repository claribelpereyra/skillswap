package com.uade.skillswap.application.eventos;

import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.booking.observer.BookingEvent;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.notificaciones.EventoNotificable;
import com.uade.skillswap.domain.notificaciones.TipoEventoNotificable;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class BookingNotificationObserverTest {
    @Test
    void mapsTheSixEventsAndUsesStableIdsForRepeatedDelivery() {
        List<EventoNotificable> received = new ArrayList<>();
        var observer = new BookingNotificationObserver(received::add);
        var statuses = List.of(BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.CONFIRMED,
                BookingStatus.ACCEPTED, BookingStatus.CANCELLED, BookingStatus.COMPLETED);
        for (int i = 0; i < statuses.size(); i++) {
            var event = new BookingEvent(10L, new UserId("ana"), new UserId("bruno"),
                    i == 0 ? null : BookingStatus.ACCEPTED, statuses.get(i), new UserId("ana"),
                    Instant.EPOCH.plusSeconds(i), i == 3 ? "Rescheduled to slot 2" : null, i == 3);
            // ACCEPTED necesita un origen PENDING; el resto conserva su transición real.
            if (i == 1) event = new BookingEvent(10L, event.clientId(), event.providerId(),
                    BookingStatus.PENDING, event.to(), event.actor(), event.occurredAt(), null);
            observer.update(event);
            observer.update(event);
        }
        assertThat(received).hasSize(12);
        for (int i = 0; i < received.size(); i += 2) {
            assertThat(received.get(i).id()).isEqualTo(received.get(i + 1).id());
            assertThat(received.get(i).destinatarios()).containsExactly("ana", "bruno");
        }
        assertThat(received.stream().map(EventoNotificable::tipo).distinct())
                .containsExactly(TipoEventoNotificable.values());
    }

    @Test
    void recognizesARescheduleByItsMarkAndNotByTheReasonText() {
        List<EventoNotificable> received = new ArrayList<>();
        var observer = new BookingNotificationObserver(received::add);

        observer.update(new BookingEvent(10, new UserId("ana"), new UserId("bruno"), BookingStatus.CONFIRMED,
                BookingStatus.CONFIRMED, new UserId("ana"), Instant.EPOCH, "Moved to another slot", true));
        observer.update(new BookingEvent(10, new UserId("ana"), new UserId("bruno"), BookingStatus.CONFIRMED,
                BookingStatus.CONFIRMED, new UserId("ana"), Instant.EPOCH.plusSeconds(1), "Rescheduled to slot 2", false));

        assertThat(received).singleElement()
                .satisfies(event -> assertThat(event.tipo()).isEqualTo(TipoEventoNotificable.REPROGRAMACION));
    }

    @Test
    void doesNotNotifyStartingRejectionOrRejectedReschedules() {
        List<EventoNotificable> received = new ArrayList<>();
        var observer = new BookingNotificationObserver(received::add);
        for (var status : List.of(BookingStatus.IN_PROGRESS, BookingStatus.REJECTED, BookingStatus.ACCEPTED)) {
            observer.update(new BookingEvent(10, new UserId("ana"), new UserId("bruno"),
                    BookingStatus.ACCEPTED, status, new UserId("bruno"), Instant.EPOCH,
                    "Reschedule to slot 2 rejected"));
        }
        assertThat(received).isEmpty();
    }
}
