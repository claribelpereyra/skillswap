package com.uade.skillswap.application.ports.in.publication;

import com.uade.skillswap.application.dto.publication.EditDraftCommand;
import com.uade.skillswap.application.dto.publication.PublicationView;

public interface EditDraftUseCase {

    PublicationView editDraft(EditDraftCommand command);
}
