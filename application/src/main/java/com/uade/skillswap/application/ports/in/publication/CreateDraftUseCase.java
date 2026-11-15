package com.uade.skillswap.application.ports.in.publication;

import com.uade.skillswap.application.dto.publication.CreateDraftCommand;
import com.uade.skillswap.application.dto.publication.PublicationView;

public interface CreateDraftUseCase {

    PublicationView createDraft(CreateDraftCommand command);
}
