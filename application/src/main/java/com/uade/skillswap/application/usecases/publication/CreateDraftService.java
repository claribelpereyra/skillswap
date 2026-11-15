package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.in.publication.CreateDraftCommand;
import com.uade.skillswap.application.ports.in.publication.CreateDraftUseCase;
import com.uade.skillswap.application.ports.in.publication.PublicationView;
import com.uade.skillswap.application.ports.in.publication.SlotInput;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.TimeProvider;
import com.uade.skillswap.application.ports.out.publication.AuthorProfileQuery;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.Publication;

import java.util.Objects;

public class CreateDraftService implements CreateDraftUseCase {

    private final PublicationRepository publications;
    private final AuthorProfileQuery profiles;
    private final UnitOfWork unitOfWork;
    private final TimeProvider timeProvider;

    public CreateDraftService(PublicationRepository publications, AuthorProfileQuery profiles,
                              UnitOfWork unitOfWork, TimeProvider timeProvider) {
        this.publications = Objects.requireNonNull(publications);
        this.profiles = Objects.requireNonNull(profiles);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.timeProvider = Objects.requireNonNull(timeProvider);
    }

    @Override
    public PublicationView createDraft(CreateDraftCommand command) {
        AccountType accountType = profiles.accountTypeOf(command.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile", command.authorId()));
        return unitOfWork.execute(() -> {
            Publication draft = Publication.draft(command.authorId(), accountType, command.type(),
                    command.details(), timeProvider.now());
            for (SlotInput slot : command.slots()) {
                draft.addSlot(command.authorId(), slot.startsAt(), slot.endsAt());
            }
            return PublicationView.from(publications.save(draft));
        });
    }
}
