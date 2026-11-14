package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationStatus;

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
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public Publication save(Publication publication) {
        saves++;
        Publication withId = publication.getId() != null ? publication
                : Publication.restore(nextId++, publication.getAuthorId(), publication.getType(),
                        publication.getDetails(), publication.getSlots(), publication.getHistory());
        stored.put(withId.getId(), withId);
        return withId;
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
                .toList();
        return new Page<>(items, page, matching.size());
    }
}
