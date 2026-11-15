package com.uade.skillswap.application.ports.in.publication;

import com.uade.skillswap.application.dto.publication.ChangePublicationStatusCommand;
import com.uade.skillswap.application.dto.publication.PublicationView;

public interface ChangePublicationStatusUseCase {

    PublicationView changeStatus(ChangePublicationStatusCommand command);
}
