package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.publication.CreateDraftCommand;
import com.uade.skillswap.application.ports.in.publication.CreateDraftUseCase;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.application.dto.publication.SlotInput;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.publication.AuthorProfileQuery;
import com.uade.skillswap.application.ports.out.publication.CategoryTree;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.Publication;

import java.time.Clock;
import java.util.Objects;

public class CreateDraftService implements CreateDraftUseCase {

    private final PublicationRepository publications;
    private final CategoryTree categories;
    private final AuthorProfileQuery profiles;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public CreateDraftService(PublicationRepository publications, AuthorProfileQuery profiles,
                              CategoryTree categories, UnitOfWork unitOfWork, Clock clock) {
        this.publications = Objects.requireNonNull(publications);
        this.profiles = Objects.requireNonNull(profiles);
        this.categories = Objects.requireNonNull(categories);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public PublicationView createDraft(CreateDraftCommand command) {
        AccountType accountType = profiles.accountTypeOf(command.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile", command.authorId()));
        requireExistingCategory(categories, command.details().categoryId());
        return unitOfWork.execute(() -> {
            Publication draft = Publication.draft(command.authorId(), accountType, command.type(),
                    command.details(), clock.instant());
            for (SlotInput slot : command.slots()) {
                draft.addSlot(command.authorId(), slot.startsAt(), slot.endsAt());
            }
            return PublicationView.from(publications.save(draft));
        });
    }

    static void requireExistingCategory(CategoryTree categories, Long categoryId) {
        if (categoryId != null && !categories.contains(categoryId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }
    }
}
