package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.ports.in.publication.ChangePublicationStatusCommand;
import com.uade.skillswap.application.ports.in.publication.ChangePublicationStatusUseCase;
import com.uade.skillswap.application.ports.in.publication.CreateDraftCommand;
import com.uade.skillswap.application.ports.in.publication.CreateDraftUseCase;
import com.uade.skillswap.application.ports.in.publication.EditDraftCommand;
import com.uade.skillswap.application.ports.in.publication.EditDraftUseCase;
import com.uade.skillswap.application.ports.in.publication.GetPublicationUseCase;
import com.uade.skillswap.application.ports.in.publication.ListMyPublicationsUseCase;
import com.uade.skillswap.application.ports.in.publication.PublicationAction;
import com.uade.skillswap.application.ports.in.publication.PublicationView;
import com.uade.skillswap.application.ports.in.publication.SlotInput;
import com.uade.skillswap.controllers.common.Result;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.PublicationType;

import java.time.Duration;
import java.util.List;

import static com.uade.skillswap.controllers.common.ControllerSupport.activeUser;
import static com.uade.skillswap.controllers.common.ControllerSupport.handle;
import static com.uade.skillswap.controllers.common.ControllerSupport.parseEnum;
import static com.uade.skillswap.controllers.common.ControllerSupport.requireEnum;

// Controller delgado (GRASP Controller): recibe la acción de la UI, valida y convierte la entrada,
// y delega en el caso de uso. No tiene SQL ni reglas de negocio: quién puede publicar, qué transición
// vale o qué falta para publicar lo decide el dominio.
public class PublicationController {

    private final CreateDraftUseCase createDraft;
    private final EditDraftUseCase editDraft;
    private final ChangePublicationStatusUseCase changeStatus;
    private final GetPublicationUseCase getPublication;
    private final ListMyPublicationsUseCase listMine;

    public PublicationController(CreateDraftUseCase createDraft, EditDraftUseCase editDraft,
                                 ChangePublicationStatusUseCase changeStatus, GetPublicationUseCase getPublication,
                                 ListMyPublicationsUseCase listMine) {
        this.createDraft = createDraft;
        this.editDraft = editDraft;
        this.changeStatus = changeStatus;
        this.getPublication = getPublication;
        this.listMine = listMine;
    }

    public Result<PublicationView> createDraft(String userId, PublicationForm form) {
        return handle(() -> createDraft.createDraft(new CreateDraftCommand(activeUser(userId),
                requireEnum(PublicationType.class, form.type(), "publication type"), details(form), slots(form))));
    }

    public Result<PublicationView> editDraft(String userId, long publicationId, PublicationForm form) {
        return handle(() -> editDraft.editDraft(new EditDraftCommand(activeUser(userId), publicationId,
                details(form), slots(form))));
    }

    // action: PUBLISH, PAUSE, RESUME, CLOSE o CANCEL (cancelar pide motivo).
    public Result<PublicationView> changeStatus(String userId, long publicationId, String action, String reason) {
        return handle(() -> changeStatus.changeStatus(new ChangePublicationStatusCommand(activeUser(userId),
                publicationId, requireEnum(PublicationAction.class, action, "action"), reason)));
    }

    public Result<PublicationView> getDetail(String userId, long publicationId) {
        return handle(() -> getPublication.get(activeUser(userId), publicationId));
    }

    // status opcional (null = todas). page arranca en 0.
    public Result<Page<PublicationView>> listMine(String userId, String status, int page, int size) {
        return handle(() -> listMine.listMine(activeUser(userId),
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
        return form.slots() == null ? List.of()
                : form.slots().stream().map(s -> new SlotInput(s.startsAt(), s.endsAt())).toList();
    }
}
