package com.uade.skillswap.application.ports.in.publication;

import com.uade.skillswap.domain.common.UserId;

public interface GetPublicationUseCase {

    // Detalle de una publicación para quien la mira. Si no la puede ver, es como si no existiera.
    PublicationView get(UserId viewerId, long publicationId);
}
