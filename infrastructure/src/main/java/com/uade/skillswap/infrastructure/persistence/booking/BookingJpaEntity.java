package com.uade.skillswap.infrastructure.persistence.booking;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Solo mapea la tabla: las reglas viven en Booking (dominio).
@Entity
@Table(name = "booking")
public class BookingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "publication_id", nullable = false)
    private long publicationId;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(name = "slot_id", nullable = false)
    private long slotId;

    @Column(name = "slot_starts_at", nullable = false)
    private Instant slotStartsAt;

    @Column(name = "slot_ends_at", nullable = false)
    private Instant slotEndsAt;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, length = 500)
    private String goal;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "proposed_slot_id")
    private Long proposedSlotId;

    @Column(name = "proposed_slot_starts_at")
    private Instant proposedSlotStartsAt;

    @Column(name = "proposed_slot_ends_at")
    private Instant proposedSlotEndsAt;

    @Column(name = "reschedule_proposed_by", length = 100)
    private String rescheduleProposedBy;

    @Version
    @Column(nullable = false)
    private long version;

    // Solo PERSIST: el historial se agrega, nunca se modifica ni se borra desde acá.
    @OneToMany(mappedBy = "booking", cascade = CascadeType.PERSIST)
    @OrderBy("position")
    private List<BookingTransitionJpaEntity> transitions = new ArrayList<>();

    protected BookingJpaEntity() {
    }

    static BookingJpaEntity create() {
        return new BookingJpaEntity();
    }

    Long getId() {
        return id;
    }

    long getPublicationId() {
        return publicationId;
    }

    void setPublicationId(long publicationId) {
        this.publicationId = publicationId;
    }

    String getClientId() {
        return clientId;
    }

    void setClientId(String clientId) {
        this.clientId = clientId;
    }

    String getProviderId() {
        return providerId;
    }

    void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    long getSlotId() {
        return slotId;
    }

    Instant getSlotStartsAt() {
        return slotStartsAt;
    }

    Instant getSlotEndsAt() {
        return slotEndsAt;
    }

    void setSlot(long slotId, Instant startsAt, Instant endsAt) {
        this.slotId = slotId;
        this.slotStartsAt = startsAt;
        this.slotEndsAt = endsAt;
    }

    String getMessage() {
        return message;
    }

    void setMessage(String message) {
        this.message = message;
    }

    String getGoal() {
        return goal;
    }

    void setGoal(String goal) {
        this.goal = goal;
    }

    long getPrice() {
        return price;
    }

    void setPrice(long price) {
        this.price = price;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    Long getProposedSlotId() {
        return proposedSlotId;
    }

    Instant getProposedSlotStartsAt() {
        return proposedSlotStartsAt;
    }

    Instant getProposedSlotEndsAt() {
        return proposedSlotEndsAt;
    }

    String getRescheduleProposedBy() {
        return rescheduleProposedBy;
    }

    void setProposal(Long slotId, Instant startsAt, Instant endsAt, String proposedBy) {
        this.proposedSlotId = slotId;
        this.proposedSlotStartsAt = startsAt;
        this.proposedSlotEndsAt = endsAt;
        this.rescheduleProposedBy = proposedBy;
    }

    List<BookingTransitionJpaEntity> getTransitions() {
        return transitions;
    }
}
