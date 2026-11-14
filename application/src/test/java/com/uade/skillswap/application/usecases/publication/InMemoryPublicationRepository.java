package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.publication.Publication;

import java.util.HashMap;
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
}
