package com.uade.skillswap.domain.booking;

import com.uade.skillswap.domain.booking.observer.BookingEvent;
import com.uade.skillswap.domain.booking.observer.BookingObserver;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class BookingObserverTest {

    private static final UserId CLIENT = new UserId("ana");
    private static final UserId PROVIDER = new UserId("bruno");
    private static final long BOOKING_ID = 99L;
    private static final TimeSlot SLOT = new TimeSlot(1L,
            Instant.parse("2026-11-19T21:00:00Z"), Instant.parse("2026-11-19T22:00:00Z"));
    private static final TimeSlot OTHER_SLOT = new TimeSlot(2L,
            Instant.parse("2026-11-20T13:00:00Z"), Instant.parse("2026-11-20T14:00:00Z"));

    private final List<BookingEvent> received = new ArrayList<>();
    private final BookingObserver recorder = received::add;

    @Test
    void observerFailuresAreLoggedAndDoNotStopOtherObservers() {
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { records.add(record); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        Logger logger = Logger.getLogger(Booking.class.getName());
        logger.addHandler(handler);
        try {
            var booking = newBooking();
            booking.addObserver(event -> { throw new IllegalStateException("channel unavailable"); });
            booking.addObserver(recorder);
            booking.publishPendingEvents(BOOKING_ID);
            assertThat(received).hasSize(1);
            assertThat(records).singleElement().satisfies(record -> {
                assertThat(record.getThrown()).isInstanceOf(IllegalStateException.class);
                assertThat(record.getMessage()).contains("booking " + BOOKING_ID, "not retried");
            });
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void persistedTimestampsAheadOfTheClockDoNotBreakTransitionsOrEvents() {
        Instant persistedTime = Instant.now().plusSeconds(60);
        Booking booking = Booking.rebuild(BOOKING_ID, 10L, CLIENT, PROVIDER, SLOT, "Hola", "Aprender",
                new SkillCoins(100), List.of(new StateTransition<>(null, BookingStatus.PENDING,
                        CLIENT, persistedTime, null)), null);
        booking.addObserver(recorder);

        booking.accept(PROVIDER);
        booking.proposeReschedule(PROVIDER, OTHER_SLOT);
        booking.acceptReschedule(CLIENT);
        booking.publishPendingEvents(BOOKING_ID);

        assertThat(received).hasSize(2).allSatisfy(event ->
                assertThat(event.occurredAt()).isEqualTo(persistedTime));
        assertThat(booking.getSlot()).isEqualTo(OTHER_SLOT);
    }

    @Test
    void nothingIsNotifiedUntilTheEventsArePublished() {
        Booking booking = newBooking();
        booking.addObserver(recorder);

        booking.accept(PROVIDER);

        assertThat(received).isEmpty();
    }

    @Test
    void publishingNotifiesEveryTransitionInOrderWithTheGivenId() {
        Booking booking = newBooking();
        booking.accept(PROVIDER);
        booking.addObserver(recorder);

        booking.publishPendingEvents(BOOKING_ID);

        assertThat(received).extracting(BookingEvent::from, BookingEvent::to)
                .containsExactly(
                        tuple(null, BookingStatus.PENDING),
                        tuple(BookingStatus.PENDING, BookingStatus.ACCEPTED));
        assertThat(received).allSatisfy(event -> {
            assertThat(event.bookingId()).isEqualTo(BOOKING_ID);
            assertThat(event.clientId()).isEqualTo(CLIENT);
            assertThat(event.providerId()).isEqualTo(PROVIDER);
        });
        assertThat(received.get(1).actor()).isEqualTo(PROVIDER);
    }

    @Test
    void eachTransitionIsNotifiedOnlyOnce() {
        Booking booking = newBooking();
        booking.addObserver(recorder);
        booking.publishPendingEvents(BOOKING_ID);

        booking.accept(PROVIDER);
        booking.publishPendingEvents(BOOKING_ID);
        booking.publishPendingEvents(BOOKING_ID);

        assertThat(received).extracting(BookingEvent::to)
                .containsExactly(BookingStatus.PENDING, BookingStatus.ACCEPTED);
    }

    @Test
    void anAcceptedRescheduleIsNotifiedWithoutChangingTheStatus() {
        Booking booking = newBooking();
        booking.accept(PROVIDER);
        booking.proposeReschedule(PROVIDER, OTHER_SLOT);
        booking.publishPendingEvents(BOOKING_ID);
        booking.addObserver(recorder);

        booking.acceptReschedule(CLIENT);
        booking.publishPendingEvents(BOOKING_ID);

        assertThat(received).singleElement().satisfies(event -> {
            assertThat(event.from()).isEqualTo(BookingStatus.ACCEPTED);
            assertThat(event.to()).isEqualTo(BookingStatus.ACCEPTED);
            assertThat(event.actor()).isEqualTo(CLIENT);
            assertThat(event.reason()).isEqualTo("Rescheduled to slot 2");
            assertThat(event.rescheduled()).isTrue();
        });
    }

    @Test
    void aRejectedRescheduleIsPublishedWithoutTheRescheduleMark() {
        Booking booking = newBooking();
        booking.accept(PROVIDER);
        booking.proposeReschedule(PROVIDER, OTHER_SLOT);
        booking.publishPendingEvents(BOOKING_ID);
        booking.addObserver(recorder);

        booking.rejectReschedule(CLIENT);
        booking.publishPendingEvents(BOOKING_ID);

        assertThat(received).singleElement().satisfies(event -> {
            assertThat(event.from()).isEqualTo(event.to());
            assertThat(event.rescheduled()).isFalse();
        });
    }

    @Test
    void aBookingLoadedFromStorageHasNothingToNotify() {
        Booking original = newBooking();
        original.accept(PROVIDER);
        Booking loaded = Booking.rebuild(BOOKING_ID, 10L, CLIENT, PROVIDER, SLOT, "Hola", "Aprender",
                new SkillCoins(100), original.getHistory(), null);
        loaded.addObserver(recorder);

        loaded.publishPendingEvents(BOOKING_ID);

        assertThat(received).isEmpty();
    }

    @Test
    void publicaLaTransicionConLaFechaAjustadaAlUltimoEstado() {
        Instant future = Instant.now().plusSeconds(3600);
        Booking booking = Booking.rebuild(BOOKING_ID, 10L, CLIENT, PROVIDER, SLOT, "Hola", "Aprender",
                new SkillCoins(100), List.of(new StateTransition<>(null, BookingStatus.PENDING, CLIENT, future, null)),
                null);
        booking.addObserver(recorder);

        booking.accept(PROVIDER);
        booking.publishPendingEvents(BOOKING_ID);

        assertThat(booking.getHistory()).last().satisfies(transition ->
                assertThat(transition.occurredAt()).isEqualTo(future));
        assertThat(received).singleElement().satisfies(event -> {
            assertThat(event.to()).isEqualTo(BookingStatus.ACCEPTED);
            assertThat(event.occurredAt()).isEqualTo(future);
        });
    }

    @Test
    void aRemovedObserverIsNotNotified() {
        Booking booking = newBooking();
        booking.addObserver(recorder);
        booking.removeObserver(recorder);

        booking.publishPendingEvents(BOOKING_ID);

        assertThat(received).isEmpty();
    }

    private static Booking newBooking() {
        return Booking.request(10L, CLIENT, PROVIDER, SLOT, "Hola", "Aprender", new SkillCoins(100));
    }
}
