package com.uade.skillswap.infrastructure.persistence.publication;

import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.TimeSlot;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Traduce entre el agregado del dominio y las entidades JPA. Es el único lugar que conoce las dos formas.
final class PublicationMapper {

    private PublicationMapper() {
    }

    static Publication toDomain(PublicationJpaEntity entity) {
        PublicationDetails details = new PublicationDetails(
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategoryId(),
                entity.getModality(),
                entity.getDurationMinutes() == null ? null : Duration.ofMinutes(entity.getDurationMinutes()),
                entity.getCapacity(),
                entity.getBasePrice() == null ? null : new SkillCoins(entity.getBasePrice()),
                entity.getTags());
        List<TimeSlot> slots = entity.getSlots().stream()
                .map(s -> new TimeSlot(s.getId(), s.getStartsAt(), s.getEndsAt(), s.isOccupied()))
                .toList();
        List<StateTransition<PublicationStatus>> history = entity.getHistory().stream()
                .map(h -> new StateTransition<>(h.getFromStatus(), h.getToStatus(), new UserId(h.getActorId()),
                        h.getOccurredAt(), h.getReason()))
                .toList();
        return Publication.restore(entity.getId(), new UserId(entity.getAuthorId()), entity.getType(), details,
                slots, history);
    }

    static PublicationJpaEntity newEntity(Publication publication) {
        Instant createdAt = publication.getHistory().get(0).occurredAt();
        PublicationJpaEntity entity = new PublicationJpaEntity(publication.getAuthorId().value(),
                publication.getType(), createdAt);
        copyInto(publication, entity);
        return entity;
    }

    // Vuelca el estado del agregado sobre una entidad ya existente (o recién creada).
    static void copyInto(Publication publication, PublicationJpaEntity entity) {
        PublicationDetails details = publication.getDetails();
        entity.setStatus(publication.getStatus());
        entity.setTitle(details.title());
        entity.setDescription(details.description());
        entity.setCategoryId(details.categoryId());
        entity.setModality(details.modality());
        entity.setDurationMinutes(details.estimatedDuration() == null
                ? null : Math.toIntExact(details.estimatedDuration().toMinutes()));
        entity.setCapacity(details.capacity());
        entity.setBasePrice(details.basePrice() == null ? null : details.basePrice().amount());
        if (!entity.getTags().equals(details.tags())) {
            entity.getTags().clear();
            entity.getTags().addAll(details.tags());
        }
        entity.setPublishedAt(lastPublishedAt(publication));
        syncSlots(publication, entity);
        appendNewTransitions(publication, entity);
    }

    // Fecha de la última vez que pasó a PUBLISHED (al reanudar una pausada vuelve a "subir" en "más recientes").
    private static Instant lastPublishedAt(Publication publication) {
        List<StateTransition<PublicationStatus>> history = publication.getHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).to() == PublicationStatus.PUBLISHED) {
                return history.get(i).occurredAt();
            }
        }
        return null;
    }

    // Las franjas se identifican por su horario (es lo que las hace iguales en el dominio).
    private static void syncSlots(Publication publication, PublicationJpaEntity entity) {
        List<TimeSlot> wanted = publication.getSlots();
        entity.getSlots().removeIf(stored -> wanted.stream()
                .noneMatch(slot -> stored.hasSchedule(slot.getStartsAt(), slot.getEndsAt())));
        for (TimeSlot slot : wanted) {
            entity.getSlots().stream()
                    .filter(stored -> stored.hasSchedule(slot.getStartsAt(), slot.getEndsAt()))
                    .findFirst()
                    .ifPresentOrElse(
                            stored -> stored.setOccupied(slot.isOccupied()),
                            () -> entity.getSlots().add(new PublicationSlotJpaEntity(entity, slot.getStartsAt(),
                                    slot.getEndsAt(), slot.isOccupied())));
        }
    }

    // El historial es inmutable: solo se agregan las transiciones nuevas, nunca se tocan las guardadas.
    private static void appendNewTransitions(Publication publication, PublicationJpaEntity entity) {
        List<StateTransition<PublicationStatus>> transitions = publication.getHistory();
        for (int position = entity.getHistory().size(); position < transitions.size(); position++) {
            StateTransition<PublicationStatus> t = transitions.get(position);
            entity.getHistory().add(new PublicationTransitionJpaEntity(entity, position, t.from(), t.to(), t.actor().value(),
                    t.occurredAt(), t.reason()));
        }
    }
}
