package com.uade.skillswap.application.ports.in.publication;

public interface ChangePublicationStatusUseCase {

    PublicationView changeStatus(ChangePublicationStatusCommand command);
}
