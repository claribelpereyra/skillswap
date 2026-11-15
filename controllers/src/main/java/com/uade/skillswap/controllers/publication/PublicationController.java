package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.ports.in.publication.PublicationView;
import com.uade.skillswap.controllers.common.Result;

// PublicacionController del enunciado. La UI conoce esta interfaz, nunca la implementación (sección 10).
public interface PublicationController {

    Result<PublicationView> createDraft(String userId, PublicationForm form);

    Result<PublicationView> editDraft(String userId, long publicationId, PublicationForm form);

    // action: PUBLISH, PAUSE, RESUME, CLOSE o CANCEL (cancelar pide motivo).
    Result<PublicationView> changeStatus(String userId, long publicationId, String action, String reason);

    Result<PublicationView> getDetail(String userId, long publicationId);

    // status opcional (null = todas). page arranca en 0.
    Result<Page<PublicationView>> listMine(String userId, String status, int page, int size);
}
