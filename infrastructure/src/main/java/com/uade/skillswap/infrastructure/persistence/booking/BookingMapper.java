package com.uade.skillswap.infrastructure.persistence.booking;

import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.booking.RescheduleProposal;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;

import java.util.List;

final class BookingMapper {

    private BookingMapper() {
    }

    static Booking toDomain(BookingJpaEntity entity) {
        List<StateTransition<BookingStatus>> history = entity.getTransitions().stream()
                .map(BookingMapper::toDomain)
                .toList();
        RescheduleProposal proposal = entity.getProposedSlotId() == null ? null : new RescheduleProposal(
                new TimeSlot(entity.getProposedSlotId(), entity.getProposedSlotStartsAt(), entity.getProposedSlotEndsAt()),
                new UserId(entity.getRescheduleProposedBy()));
        return Booking.rebuild(
                entity.getId(),
                entity.getPublicationId(),
                new UserId(entity.getClientId()),
                new UserId(entity.getProviderId()),
                new TimeSlot(entity.getSlotId(), entity.getSlotStartsAt(), entity.getSlotEndsAt()),
                entity.getMessage(),
                entity.getGoal(),
                new SkillCoins(entity.getPrice()),
                history,
                proposal);
    }

    // El historial es inmutable: solo se agregan las transiciones que todavía no están guardadas.
    static void copyToEntity(Booking booking, BookingJpaEntity entity) {
        entity.setPublicationId(booking.getPublicationId());
        entity.setClientId(booking.getClientId().value());
        entity.setProviderId(booking.getProviderId().value());
        entity.setSlot(booking.getSlot().getId(), booking.getSlot().getStartsAt(), booking.getSlot().getEndsAt());
        entity.setMessage(booking.getMessage());
        entity.setGoal(booking.getGoal());
        entity.setPrice(booking.getPrice().amount());
        entity.setStatus(booking.getStatus().name());
        booking.getPendingReschedule().ifPresentOrElse(
                proposal -> entity.setProposal(proposal.newSlot().getId(), proposal.newSlot().getStartsAt(),
                        proposal.newSlot().getEndsAt(), proposal.proposedBy().value()),
                () -> entity.setProposal(null, null, null, null));

        List<StateTransition<BookingStatus>> history = booking.getHistory();
        for (int position = entity.getTransitions().size(); position < history.size(); position++) {
            StateTransition<BookingStatus> transition = history.get(position);
            entity.getTransitions().add(new BookingTransitionJpaEntity(
                    entity,
                    position,
                    transition.from() == null ? null : transition.from().name(),
                    transition.to().name(),
                    transition.actor().value(),
                    transition.occurredAt(),
                    transition.reason()));
        }
    }

    private static StateTransition<BookingStatus> toDomain(BookingTransitionJpaEntity transition) {
        return new StateTransition<>(
                transition.getFromStatus() == null ? null : BookingStatus.valueOf(transition.getFromStatus()),
                BookingStatus.valueOf(transition.getToStatus()),
                new UserId(transition.getActorId()),
                transition.getOccurredAt(),
                transition.getReason());
    }
}
