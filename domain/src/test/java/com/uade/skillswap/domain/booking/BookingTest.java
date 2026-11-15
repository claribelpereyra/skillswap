package com.uade.skillswap.domain.booking;

import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.InvalidTransitionException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private static final UserId CLIENT = new UserId("ana");
    private static final UserId PROVIDER = new UserId("bruno");
    private static final UserId OUTSIDER = new UserId("carla");
    private static final long PUBLICATION_ID = 10L;
    private static final SkillCoins PRICE = new SkillCoins(100);
    private static final TimeSlot SLOT = new TimeSlot(1L,
            Instant.parse("2026-11-19T21:00:00Z"), Instant.parse("2026-11-19T22:00:00Z"));

    // Solicitar

    @Test
    void aValidRequestStartsPendingWithTheClientInTheHistory() {
        Booking booking = newBooking();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getPrice()).isEqualTo(PRICE);
        assertThat(booking.getSlot()).isSameAs(SLOT);
        assertThat(booking.getHistory()).singleElement().satisfies(creation -> {
            assertThat(creation.from()).isNull();
            assertThat(creation.to()).isEqualTo(BookingStatus.PENDING);
            assertThat(creation.actor()).isEqualTo(CLIENT);
        });
    }

    @Test
    void theAuthorCannotRequestTheirOwnPublication() {
        assertThatThrownBy(() -> Booking.request(PUBLICATION_ID, PROVIDER, PROVIDER, SLOT, "Hola", "Aprender", PRICE))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The author cannot request their own publication");
    }

    @Test
    void aRequestNeedsASlotAMessageAndAGoal() {
        assertThatThrownBy(() -> Booking.request(PUBLICATION_ID, CLIENT, PROVIDER, null, "Hola", "Aprender", PRICE))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A slot is required to request a publication");
        assertThatThrownBy(() -> Booking.request(PUBLICATION_ID, CLIENT, PROVIDER, SLOT, " ", "Aprender", PRICE))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A message is required to request a publication");
        assertThatThrownBy(() -> Booking.request(PUBLICATION_ID, CLIENT, PROVIDER, SLOT, "Hola", null, PRICE))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A goal is required to request a publication");
    }

    // Transiciones válidas

    @Test
    void theProviderAcceptsAPendingBooking() {
        Booking booking = newBooking();

        booking.accept(PROVIDER);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
        assertLastTransition(booking, BookingStatus.PENDING, BookingStatus.ACCEPTED, PROVIDER, null);
    }

    @Test
    void theProviderRejectsAPendingBookingWithAReason() {
        Booking booking = newBooking();

        booking.reject(PROVIDER, "No tengo disponibilidad");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertLastTransition(booking, BookingStatus.PENDING, BookingStatus.REJECTED, PROVIDER, "No tengo disponibilidad");
    }

    @Test
    void theClientPaysAnAcceptedBooking() {
        Booking booking = accepted();

        booking.confirmPayment(CLIENT);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertLastTransition(booking, BookingStatus.ACCEPTED, BookingStatus.CONFIRMED, CLIENT, null);
    }

    @Test
    void theProviderStartsAConfirmedBooking() {
        Booking booking = confirmed();

        booking.start(PROVIDER);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.IN_PROGRESS);
        assertLastTransition(booking, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS, PROVIDER, null);
    }

    @Test
    void eitherParticipantCompletesABookingInProgress() {
        Booking completedByClient = inProgress();
        Booking completedByProvider = inProgress();

        completedByClient.complete(CLIENT);
        completedByProvider.complete(PROVIDER);

        assertThat(completedByClient.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(completedByProvider.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertLastTransition(completedByClient, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED, CLIENT, null);
    }

    @Test
    void eitherParticipantCancelsAnAcceptedBookingWithAReason() {
        Booking booking = accepted();

        booking.cancel(CLIENT, "Me surgió un viaje");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertLastTransition(booking, BookingStatus.ACCEPTED, BookingStatus.CANCELLED, CLIENT, "Me surgió un viaje");
    }

    @Test
    void eitherParticipantCancelsAConfirmedBookingWithAReason() {
        Booking booking = confirmed();

        booking.cancel(PROVIDER, "Estoy enfermo");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertLastTransition(booking, BookingStatus.CONFIRMED, BookingStatus.CANCELLED, PROVIDER, "Estoy enfermo");
    }

    @Test
    void theHistoryKeepsEveryTransitionInOrder() {
        Booking booking = inProgress();

        booking.complete(CLIENT);

        assertThat(booking.getHistory()).extracting(StateTransition::to).containsExactly(
                BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.CONFIRMED,
                BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED);
        assertThat(booking.getHistory()).extracting(StateTransition::actor).containsExactly(
                CLIENT, PROVIDER, CLIENT, PROVIDER, CLIENT);
    }

    // Roles

    @Test
    void onlyTheProviderCanAcceptOrReject() {
        Booking booking = newBooking();

        assertThatThrownBy(() -> booking.accept(CLIENT))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("Only the provider can accept a booking");
        assertThatThrownBy(() -> booking.reject(CLIENT, "No"))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("Only the provider can reject a booking");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void onlyTheClientCanPay() {
        Booking booking = accepted();

        assertThatThrownBy(() -> booking.confirmPayment(PROVIDER))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("Only the client can pay a booking");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
    }

    @Test
    void onlyTheProviderCanStart() {
        Booking booking = confirmed();

        assertThatThrownBy(() -> booking.start(CLIENT))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("Only the provider can start a booking");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void someoneOutsideTheBookingCannotCompleteOrCancel() {
        Booking inProgress = inProgress();
        Booking accepted = accepted();

        assertThatThrownBy(() -> inProgress.complete(OUTSIDER))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("Only the client or the provider can complete a booking");
        assertThatThrownBy(() -> accepted.cancel(OUTSIDER, "Porque sí"))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("Only the client or the provider can cancel a booking");
    }

    // Motivo obligatorio

    @Test
    void rejectingAndCancellingNeedAReason() {
        Booking pending = newBooking();
        Booking accepted = accepted();

        assertThatThrownBy(() -> pending.reject(PROVIDER, " "))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A reason is required to reject a booking");
        assertThatThrownBy(() -> accepted.cancel(CLIENT, null))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A reason is required to cancel a booking");
        assertThat(pending.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(accepted.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
    }

    // Orden de los chequeos: primero el estado, después el rol

    @Test
    void theStateIsCheckedBeforeTheRole() {
        Booking booking = confirmed();

        assertThatThrownBy(() -> booking.accept(CLIENT))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessage("Cannot accept in state CONFIRMED");
    }

    // Transiciones inválidas

    @Test
    void aPendingBookingCannotBePaidStartedOrCancelled() {
        Booking booking = newBooking();

        assertThatThrownBy(() -> booking.confirmPayment(CLIENT))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessage("Cannot pay in state PENDING");
        assertThatThrownBy(() -> booking.start(PROVIDER))
                .isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> booking.cancel(CLIENT, "Me arrepentí"))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessage("Cannot cancel in state PENDING");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getHistory()).hasSize(1);
    }

    @Test
    void aBookingCannotBePaidTwice() {
        Booking booking = confirmed();

        assertThatThrownBy(() -> booking.confirmPayment(CLIENT))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessage("Cannot pay in state CONFIRMED");
    }

    @Test
    void anAcceptedBookingCannotBeAcceptedAgainNorStarted() {
        Booking booking = accepted();

        assertThatThrownBy(() -> booking.accept(PROVIDER)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> booking.start(PROVIDER)).isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void aBookingInProgressCannotBeCancelled() {
        Booking booking = inProgress();

        assertThatThrownBy(() -> booking.cancel(CLIENT, "Ya no quiero"))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessage("Cannot cancel in state IN_PROGRESS");
    }

    @Test
    void finalStatesRejectEveryAction() {
        Booking completed = completed();
        Booking rejected = newBooking();
        rejected.reject(PROVIDER, "No puedo");
        Booking cancelled = accepted();
        cancelled.cancel(CLIENT, "No puedo");

        for (Booking finished : List.of(completed, rejected, cancelled)) {
            BookingStatus status = finished.getStatus();
            everyAction().forEach((action, attempt) ->
                    assertThatThrownBy(() -> attempt.accept(finished))
                            .as("%s in %s", action, status)
                            .isInstanceOf(InvalidTransitionException.class));
            assertThat(finished.getStatus()).isEqualTo(status);
        }
    }

    private static Map<String, Consumer<Booking>> everyAction() {
        return Map.of(
                "accept", b -> b.accept(PROVIDER),
                "reject", b -> b.reject(PROVIDER, "Motivo"),
                "pay", b -> b.confirmPayment(CLIENT),
                "start", b -> b.start(PROVIDER),
                "complete", b -> b.complete(CLIENT),
                "cancel", b -> b.cancel(CLIENT, "Motivo"));
    }

    private static void assertLastTransition(Booking booking, BookingStatus from, BookingStatus to,
                                             UserId actor, String reason) {
        StateTransition<BookingStatus> last = booking.getHistory().get(booking.getHistory().size() - 1);
        assertThat(last.from()).isEqualTo(from);
        assertThat(last.to()).isEqualTo(to);
        assertThat(last.actor()).isEqualTo(actor);
        assertThat(last.reason()).isEqualTo(reason);
        assertThat(last.occurredAt()).isNotNull();
    }

    private static Booking newBooking() {
        return Booking.request(PUBLICATION_ID, CLIENT, PROVIDER, SLOT, "Hola Bruno", "Preparar una entrevista", PRICE);
    }

    private static Booking accepted() {
        Booking booking = newBooking();
        booking.accept(PROVIDER);
        return booking;
    }

    private static Booking confirmed() {
        Booking booking = accepted();
        booking.confirmPayment(CLIENT);
        return booking;
    }

    private static Booking inProgress() {
        Booking booking = confirmed();
        booking.start(PROVIDER);
        return booking;
    }

    private static Booking completed() {
        Booking booking = inProgress();
        booking.complete(CLIENT);
        return booking;
    }
}
