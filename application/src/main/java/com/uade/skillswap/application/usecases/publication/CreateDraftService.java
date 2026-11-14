package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.in.publication.CreateDraftCommand;
import com.uade.skillswap.application.ports.in.publication.CreateDraftUseCase;
import com.uade.skillswap.application.ports.in.publication.PublicationView;
import com.uade.skillswap.application.ports.in.publication.SlotInput;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.publication.AuthorProfileQuery;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.Publication;

import java.time.Clock;

public class CreateDraftService implements CreateDraftUseCase {

    private final PublicationRepository publications;
    private final AuthorProfileQuery profiles;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public CreateDraftService(PublicationRepository publications, AuthorProfileQuery profiles,
                              UnitOfWork unitOfWork, Clock clock) {
        this.publications = publications;
        this.profiles = profiles;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public PublicationView createDraft(CreateDraftCommand command) {
        AccountType accountType = profiles.accountTypeOf(command.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile", command.authorId()));
        return unitOfWork.execute(() -> {
            Publication draft = Publication.draft(command.authorId(), accountType, command.type(),
                    command.details(), clock.instant());
            for (SlotInput slot : command.slots()) {
                draft.addSlot(command.authorId(), slot.startsAt(), slot.endsAt());
            }
            return PublicationView.from(publications.save(draft));
        });
    }
}
