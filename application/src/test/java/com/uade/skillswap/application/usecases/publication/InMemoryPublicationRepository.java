package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.TimeSlot;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Puerto simulado: guarda en memoria y asigna ids como lo haría la base.
class InMemoryPublicationRepository implements PublicationRepository {

    private final Map<Long, Publication> stored = new HashMap<>();
    private long nextId = 1;
    int saves;

    @Override
    public Optional<Publication> findById(long id) {
        return Optional.ofNullable(stored.get(id)).map(InMemoryPublicationRepository::copy);
    }

    // Guarda una copia, igual que una base: si el caso de uso cambia la publicación y después falla
    // sin llamar a save, lo guardado no cambia.
    @Override
    public Publication save(Publication publication) {
        saves++;
        long id = publication.getId() != null ? publication.getId() : nextId++;
        stored.put(id, copy(publication, id));
        return copy(stored.get(id));
    }

    @Override
    public Page<Publication> findByAuthor(UserId authorId, PublicationStatus status, PageRequest page) {
        List<Publication> matching = stored.values().stream()
                .filter(p -> p.getAuthorId().equals(authorId))
                .filter(p -> status == null || p.getStatus() == status)
                .sorted(Comparator.comparing(Publication::getId).reversed())
                .toList();
        List<Publication> items = matching.stream()
                .skip(page.offset())
                .limit(page.size())
                .map(InMemoryPublicationRepository::copy)
                .toList();
        return new Page<>(items, page, matching.size());
    }

    private static Publication copy(Publication p) {
        return copy(p, p.getId());
    }

    private static Publication copy(Publication p, long id) {
        List<TimeSlot> slots = p.getSlots().stream()
                .map(s -> new TimeSlot(s.getId(), s.getStartsAt(), s.getEndsAt(), s.isOccupied()))
                .toList();
        return Publication.restore(id, p.getAuthorId(), p.getType(), p.getDetails(), slots, p.getHistory());
    }
}
