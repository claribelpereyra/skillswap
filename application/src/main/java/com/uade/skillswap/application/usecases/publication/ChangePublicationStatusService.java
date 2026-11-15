package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.publication.ChangePublicationStatusCommand;
import com.uade.skillswap.application.ports.in.publication.ChangePublicationStatusUseCase;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Publication;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class ChangePublicationStatusService implements ChangePublicationStatusUseCase {

    private final PublicationRepository publications;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public ChangePublicationStatusService(PublicationRepository publications, UnitOfWork unitOfWork, Clock clock) {
        this.publications = Objects.requireNonNull(publications);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.clock = Objects.requireNonNull(clock);
    }

    // Solo traduce la acción pedida al método del agregado. Si la transición es válida o no,
    // lo decide el estado de la publicación (patrón State), no este caso de uso.
    @Override
    public PublicationView changeStatus(ChangePublicationStatusCommand command) {
        return unitOfWork.execute(() -> {
            Publication publication = publications.findById(command.publicationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Publication", command.publicationId()));
            UserId actor = command.actorId();
            Instant now = clock.instant();
            switch (command.action()) {
                case PUBLISH -> publication.publish(actor, now);
                case PAUSE -> publication.pause(actor, now);
                case RESUME -> publication.resume(actor, now);
                case CLOSE -> publication.close(actor, now);
                case CANCEL -> publication.cancel(actor, command.reason(), now);
            }
            return PublicationView.from(publications.save(publication));
        });
    }
}
