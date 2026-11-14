package com.uade.skillswap.application.ports.in.publication;

public interface CreateDraftUseCase {

    PublicationView createDraft(CreateDraftCommand command);
}
