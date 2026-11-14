package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.in.publication.EditDraftCommand;
import com.uade.skillswap.application.ports.in.publication.EditDraftUseCase;
import com.uade.skillswap.application.ports.in.publication.PublicationView;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.TimeSlot;

import java.util.List;
import java.util.Objects;

public class EditDraftService implements EditDraftUseCase {

    private final PublicationRepository publications;
    private final UnitOfWork unitOfWork;

    public EditDraftService(PublicationRepository publications, UnitOfWork unitOfWork) {
        this.publications = Objects.requireNonNull(publications);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
    }

    @Override
    public PublicationView editDraft(EditDraftCommand command) {
        return unitOfWork.execute(() -> {
            Publication publication = publications.findById(command.publicationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Publication", command.publicationId()));
            publication.edit(command.actorId(), command.details());
            replaceSlots(publication, command);
            return PublicationView.from(publications.save(publication));
        });
    }

    // Deja exactamente las franjas pedidas: saca las que ya no están y agrega las nuevas.
    // Primero saca, así una franja que se corre de horario no choca con su versión anterior.
    private void replaceSlots(Publication publication, EditDraftCommand command) {
        List<TimeSlot> wanted = command.slots().stream()
                .map(s -> new TimeSlot(null, s.startsAt(), s.endsAt()))
                .toList();
        for (TimeSlot current : publication.getSlots()) {
            if (!wanted.contains(current)) {
                publication.removeSlot(command.actorId(), current);
            }
        }
        List<TimeSlot> kept = publication.getSlots();
        for (TimeSlot slot : wanted) {
            if (!kept.contains(slot)) {
                publication.addSlot(command.actorId(), slot.getStartsAt(), slot.getEndsAt());
            }
        }
    }
}
