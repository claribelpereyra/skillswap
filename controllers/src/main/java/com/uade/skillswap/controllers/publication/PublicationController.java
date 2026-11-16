package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.controllers.ControllerResult;

// PublicacionController del enunciado. La UI conoce esta interfaz, nunca la implementación (sección 10).
public interface PublicationController {

    ControllerResult<PublicationView> createDraft(String userId, PublicationForm form);

    ControllerResult<PublicationView> editDraft(String userId, long publicationId, PublicationForm form);

    // action: PUBLISH, PAUSE, RESUME, CLOSE o CANCEL (cancelar pide motivo).
    ControllerResult<PublicationView> changeStatus(String userId, long publicationId, String action, String reason);

    ControllerResult<PublicationView> getDetail(String userId, long publicationId);

    // status opcional (null = todas). page arranca en 0.
    ControllerResult<Page<PublicationView>> listMine(String userId, String status, int page, int size);
}
