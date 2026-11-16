package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.ChangePublicationStatusCommand;
import com.uade.skillswap.application.dto.publication.CreateDraftCommand;
import com.uade.skillswap.application.dto.publication.EditDraftCommand;
import com.uade.skillswap.application.dto.publication.PublicationAction;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.application.dto.publication.SlotInput;
import com.uade.skillswap.application.ports.in.publication.ChangePublicationStatusUseCase;
import com.uade.skillswap.application.ports.in.publication.CreateDraftUseCase;
import com.uade.skillswap.application.ports.in.publication.EditDraftUseCase;
import com.uade.skillswap.application.ports.in.publication.GetPublicationUseCase;
import com.uade.skillswap.application.ports.in.publication.ListMyPublicationsUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.PublicationType;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.uade.skillswap.controllers.common.ControllerSupport.InvalidInputException;

import static com.uade.skillswap.controllers.common.ControllerSupport.parseEnum;
import static com.uade.skillswap.controllers.common.ControllerSupport.requireEnum;

// Controller delgado: valida el usuario activo y la entrada, y delega; las reglas las decide el dominio.
public class PublicationControllerImpl implements PublicationController {

    private final CreateDraftUseCase createDraft;
    private final EditDraftUseCase editDraft;
    private final ChangePublicationStatusUseCase changeStatus;
    private final GetPublicationUseCase getPublication;
    private final ListMyPublicationsUseCase listMine;
    private final IdentityService identity;

    public PublicationControllerImpl(CreateDraftUseCase createDraft, EditDraftUseCase editDraft,
                                     ChangePublicationStatusUseCase changeStatus,
                                     GetPublicationUseCase getPublication, ListMyPublicationsUseCase listMine,
                                     IdentityService identity) {
        this.createDraft = Objects.requireNonNull(createDraft);
        this.editDraft = Objects.requireNonNull(editDraft);
        this.changeStatus = Objects.requireNonNull(changeStatus);
        this.getPublication = Objects.requireNonNull(getPublication);
        this.listMine = Objects.requireNonNull(listMine);
        this.identity = Objects.requireNonNull(identity);
    }

    @Override
    public ControllerResult<PublicationView> createDraft(String userId, PublicationForm form) {
        return ControllerResult.of(() -> createDraft.createDraft(new CreateDraftCommand(currentUser(userId, "create a publication"),
                requireEnum(PublicationType.class, required(form).type(), "publication type"), details(form),
                slots(form))));
    }

    @Override
    public ControllerResult<PublicationView> editDraft(String userId, long publicationId, PublicationForm form) {
        return ControllerResult.of(() -> editDraft.editDraft(new EditDraftCommand(currentUser(userId, "edit a publication"), publicationId,
                details(required(form)), slots(form))));
    }

    @Override
    public ControllerResult<PublicationView> changeStatus(String userId, long publicationId, String action, String reason) {
        return ControllerResult.of(() -> changeStatus.changeStatus(new ChangePublicationStatusCommand(currentUser(userId, "change a publication status"),
                publicationId, requireEnum(PublicationAction.class, action, "action"), reason)));
    }

    @Override
    public ControllerResult<PublicationView> getDetail(String userId, long publicationId) {
        return ControllerResult.of(() -> getPublication.get(currentUser(userId, "view a publication"), publicationId));
    }

    @Override
    public ControllerResult<Page<PublicationView>> listMine(String userId, String status, int page, int size) {
        return ControllerResult.of(() -> listMine.listMine(currentUser(userId, "list their publications"),
                parseEnum(PublicationStatus.class, status, "status"), new PageRequest(page, size)));
    }

    private static PublicationDetails details(PublicationForm form) {
        return new PublicationDetails(form.title(), form.description(), form.categoryId(),
                parseEnum(Modality.class, form.modality(), "modality"),
                form.durationMinutes() == null ? null : Duration.ofMinutes(form.durationMinutes()),
                form.capacity(),
                form.basePrice() == null ? null : new SkillCoins(form.basePrice()),
                form.tags());
    }

    private static List<SlotInput> slots(PublicationForm form) {
        if (form.slots() == null) {
            return List.of();
        }
        return form.slots().stream().map(s -> {
            if (s == null || s.startsAt() == null || s.endsAt() == null) {
                throw new InvalidInputException("Every slot needs a start and an end");
            }
            return new SlotInput(s.startsAt(), s.endsAt());
        }).toList();
    }

    // Mismo chequeo que BookingControllerImpl: el userId tiene que ser el usuario activo del login simulado.
    private UserId currentUser(String userId, String operation) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("No active user");
        }
        UserId user = new UserId(userId);
        if (!user.equals(identity.currentUserId())) {
            throw new AccessDeniedException(user, operation);
        }
        return user;
    }

    private static PublicationForm required(PublicationForm form) {
        if (form == null) {
            throw new InvalidInputException("The publication form is required");
        }
        return form;
    }
}
