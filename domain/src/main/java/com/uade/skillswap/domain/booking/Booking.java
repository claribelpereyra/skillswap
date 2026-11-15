package com.uade.skillswap.domain.booking;

import com.uade.skillswap.domain.booking.state.BookingState;
import com.uade.skillswap.domain.booking.state.BookingStateFactory;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.StateHistory;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Booking {

    private Long id;
    private final long publicationId;
    private final UserId clientId;
    private final UserId providerId;
    private final String message;
    private final String goal;
    private final SkillCoins price;
    private final StateHistory<BookingStatus> history;
    private TimeSlot slot;
    private BookingState state;

    private Booking(long publicationId, UserId clientId, UserId providerId, TimeSlot slot,
                    String message, String goal, SkillCoins price, StateHistory<BookingStatus> history) {
        this.publicationId = publicationId;
        this.clientId = Objects.requireNonNull(clientId, "The client is required");
        this.providerId = Objects.requireNonNull(providerId, "The provider is required");
        this.slot = Objects.requireNonNull(slot, "The slot is required");
        this.message = message;
        this.goal = goal;
        this.price = Objects.requireNonNull(price, "The price is required");
        this.history = history;
        this.state = BookingStateFactory.fromStatus(history.currentState());
    }

    public static Booking request(long publicationId, UserId clientId, UserId providerId, TimeSlot slot,
                                  String message, String goal, SkillCoins price) {
        Objects.requireNonNull(clientId, "The client is required");
        if (clientId.equals(providerId)) {
            throw new BusinessRuleException("The author cannot request their own publication");
        }
        if (slot == null) {
            throw new BusinessRuleException("A slot is required to request a publication");
        }
        requireText(message, "A message is required to request a publication");
        requireText(goal, "A goal is required to request a publication");
        StateHistory<BookingStatus> history = StateHistory.startingAt(BookingStatus.PENDING, clientId, Instant.now());
        return new Booking(publicationId, clientId, providerId, slot, message, goal, price, history);
    }

    // State Pattern: la reserva delega cada acción en su estado actual.
    public void accept(UserId actor) {
        state.accept(this, actor);
    }

    public void reject(UserId actor, String reason) {
        state.reject(this, actor, reason);
    }

    public void confirmPayment(UserId actor) {
        state.confirmPayment(this, actor);
    }

    public void start(UserId actor) {
        state.start(this, actor);
    }

    public void complete(UserId actor) {
        state.complete(this, actor);
    }

    public void cancel(UserId actor, String reason) {
        state.cancel(this, actor, reason);
    }

    public void changeState(BookingState next, UserId actor, String reason) {
        history.record(state.getStatus(), next.getStatus(), actor, Instant.now(), reason);
        state = next;
    }

    public boolean isParticipant(UserId user) {
        return clientId.equals(user) || providerId.equals(user);
    }

    public void requireProvider(UserId actor, String action) {
        if (!providerId.equals(actor)) {
            throw new BusinessRuleException("Only the provider can " + action + " a booking");
        }
    }

    public void requireClient(UserId actor, String action) {
        if (!clientId.equals(actor)) {
            throw new BusinessRuleException("Only the client can " + action + " a booking");
        }
    }

    public void requireParticipant(UserId actor, String action) {
        if (!isParticipant(actor)) {
            throw new BusinessRuleException("Only the client or the provider can " + action + " a booking");
        }
    }

    public void requireReason(String reason, String action) {
        requireText(reason, "A reason is required to " + action + " a booking");
    }

    // Lo comparten ACCEPTED y CONFIRMED antes de pasar a CANCELLED.
    public void prepareCancellation(UserId actor, String reason) {
        requireParticipant(actor, "cancel");
        requireReason(reason, "cancel");
    }

    private static void requireText(String value, String error) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(error);
        }
    }

    public Long getId() {
        return id;
    }

    public long getPublicationId() {
        return publicationId;
    }

    public UserId getClientId() {
        return clientId;
    }

    public UserId getProviderId() {
        return providerId;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public String getMessage() {
        return message;
    }

    public String getGoal() {
        return goal;
    }

    public SkillCoins getPrice() {
        return price;
    }

    public BookingStatus getStatus() {
        return state.getStatus();
    }

    public List<StateTransition<BookingStatus>> getHistory() {
        return history.transitions();
    }
}
