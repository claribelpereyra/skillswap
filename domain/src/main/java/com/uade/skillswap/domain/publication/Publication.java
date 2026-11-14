package com.uade.skillswap.domain.publication;

import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.PermissionDeniedException;
import com.uade.skillswap.domain.common.StateHistory;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Agregado Publicación: oferta de mentoría o servicio de un profesional, o desafío de una organización.
// El cupo es por publicación (cantidad de contrataciones activas) y cada franja la toma una sola
// contratación (decisión D5).
public class Publication {

    private final Long id;
    private final UserId authorId;
    private final PublicationType type;
    private PublicationDetails details;
    private final List<TimeSlot> slots;
    private final StateHistory<PublicationStatus> history;
    private PublicationState state;

    private Publication(Long id, UserId authorId, PublicationType type, PublicationDetails details,
                        List<TimeSlot> slots, StateHistory<PublicationStatus> history) {
        this.id = id;
        this.authorId = Objects.requireNonNull(authorId, "The author is required");
        this.type = Objects.requireNonNull(type, "The publication type is required");
        this.details = Objects.requireNonNull(details, "The publication details are required");
        this.slots = new ArrayList<>(slots);
        this.history = history;
        this.state = PublicationState.of(history.currentState());
    }

    // Crea la publicación en borrador. Los datos pueden estar incompletos hasta publicar.
    public static Publication draft(UserId authorId, AccountType authorAccountType, PublicationType type,
                                    PublicationDetails details, Instant now) {
        Objects.requireNonNull(type, "The publication type is required");
        if (!type.canBePublishedBy(authorAccountType)) {
            throw new BusinessRuleException("A " + authorAccountType + " account cannot publish a " + type);
        }
        StateHistory<PublicationStatus> history = StateHistory.startingAt(PublicationStatus.DRAFT, authorId, now);
        return new Publication(null, authorId, type, details, List.of(), history);
    }

    // Reconstruye una publicación guardada (lo usa el mapper de persistencia).
    public static Publication restore(Long id, UserId authorId, PublicationType type, PublicationDetails details,
                                      List<TimeSlot> slots,
                                      List<StateTransition<PublicationStatus>> history) {
        return new Publication(id, authorId, type, details, slots, StateHistory.rebuild(history));
    }

    // --- Edición (solo en borrador) ---

    public void edit(UserId actor, PublicationDetails newDetails) {
        requireAuthor(actor, "edit");
        state.checkEditable();
        details = Objects.requireNonNull(newDetails, "The publication details are required");
    }

    public TimeSlot addSlot(UserId actor, Instant startsAt, Instant endsAt) {
        requireAuthor(actor, "add a slot to");
        state.checkEditable();
        TimeSlot slot = new TimeSlot(null, startsAt, endsAt);
        if (slots.stream().anyMatch(slot::overlaps)) {
            throw new BusinessRuleException("The slot overlaps another slot of the publication");
        }
        slots.add(slot);
        return slot;
    }

    public void removeSlot(UserId actor, TimeSlot slot) {
        requireAuthor(actor, "remove a slot from");
        state.checkEditable();
        if (!slots.remove(slot)) {
            throw new BusinessRuleException("The slot does not belong to the publication");
        }
    }

    // --- Transiciones de estado ---

    public void publish(UserId actor, Instant now) {
        requireAuthor(actor, "publish");
        transitionTo(state.publish(this), actor, now, null);
    }

    public void pause(UserId actor, Instant now) {
        requireAuthor(actor, "pause");
        transitionTo(state.pause(), actor, now, null);
    }

    public void resume(UserId actor, Instant now) {
        requireAuthor(actor, "resume");
        transitionTo(state.resume(), actor, now, null);
    }

    public void close(UserId actor, Instant now) {
        requireAuthor(actor, "close");
        transitionTo(state.close(), actor, now, null);
    }

    public void cancel(UserId actor, String reason, Instant now) {
        requireAuthor(actor, "cancel");
        PublicationState next = state.cancel();
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("A reason is required to cancel a publication");
        }
        transitionTo(next, actor, now, reason);
    }

    private void transitionTo(PublicationState next, UserId actor, Instant now, String reason) {
        history.record(state.status(), next.status(), actor, now, reason);
        state = next;
    }

    // Publicar requiere título, categoría, precio no negativo y al menos una franja (caso de aceptación 2).
    // El precio no puede ser negativo porque SkillCoins no lo admite; acá solo falta chequear que esté.
    void checkCompleteForPublishing() {
        List<String> missing = new ArrayList<>();
        if (details.title() == null || details.title().isBlank()) {
            missing.add("title");
        }
        if (details.categoryId() == null) {
            missing.add("category");
        }
        if (details.basePrice() == null) {
            missing.add("base price");
        }
        if (slots.isEmpty()) {
            missing.add("at least one time slot");
        }
        if (!missing.isEmpty()) {
            throw new IncompletePublicationException(missing);
        }
    }

    // --- Visibilidad y solicitudes ---

    // Publicada la ven todos; en cualquier otro estado, solo su autor.
    public boolean isVisibleTo(UserId viewer) {
        return state.isPublic() || authorId.equals(viewer);
    }

    public boolean acceptsRequests() {
        return state.acceptsRequests();
    }

    // Reglas para que un cliente pueda solicitar la publicación en una franja.
    public void checkCanBeRequested(UserId clientId, TimeSlot slot) {
        if (authorId.equals(clientId)) {
            throw new BusinessRuleException("The author cannot request their own publication");
        }
        if (!state.acceptsRequests()) {
            throw new BusinessRuleException("The publication does not accept requests in state " + state.status());
        }
        checkSlotIsFree(slot);
    }

    // --- Cupo y franjas ---

    // Ocupa la franja y un lugar del cupo. Lo usa la aceptación de una contratación.
    public void reserve(TimeSlot slot) {
        if (!state.allowsReservations()) {
            throw new BusinessRuleException("The publication does not allow reservations in state " + state.status());
        }
        TimeSlot own = checkSlotIsFree(slot);
        own.occupy();
    }

    // Libera la franja y el lugar. Lo usan la cancelación y la reprogramación de una contratación.
    // Se puede en cualquier estado: si se cancela una contratación de una publicación ya cerrada, el lugar vuelve.
    public void release(TimeSlot slot) {
        TimeSlot own = findSlot(slot)
                .orElseThrow(() -> new BusinessRuleException("The slot does not belong to the publication"));
        if (!own.isOccupied()) {
            throw new BusinessRuleException("The slot is not occupied");
        }
        own.release();
    }

    private TimeSlot checkSlotIsFree(TimeSlot slot) {
        TimeSlot own = findSlot(slot)
                .orElseThrow(() -> new BusinessRuleException("The slot does not belong to the publication"));
        if (own.isOccupied()) {
            throw new BusinessRuleException("The slot is already taken");
        }
        // <= 0 y no == 0: si la base trae más franjas ocupadas que cupo, igual se bloquea.
        if (availableSpots() <= 0) {
            throw new BusinessRuleException("The publication has no capacity left");
        }
        return own;
    }

    private Optional<TimeSlot> findSlot(TimeSlot slot) {
        return slots.stream().filter(s -> s.equals(slot)).findFirst();
    }

    public int availableSpots() {
        return details.capacity() - getReservedSpots();
    }

    private void requireAuthor(UserId actor, String action) {
        if (!authorId.equals(actor)) {
            throw new PermissionDeniedException("Only the author can " + action + " a publication");
        }
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public UserId getAuthorId() {
        return authorId;
    }

    public PublicationType getType() {
        return type;
    }

    public PublicationDetails getDetails() {
        return details;
    }

    public List<TimeSlot> getSlots() {
        return List.copyOf(slots);
    }

    // Cada contratación activa ocupa una franja, así que los lugares reservados son las franjas ocupadas.
    public int getReservedSpots() {
        return (int) slots.stream().filter(TimeSlot::isOccupied).count();
    }

    public PublicationStatus getStatus() {
        return state.status();
    }

    public List<StateTransition<PublicationStatus>> getHistory() {
        return history.transitions();
    }
}
