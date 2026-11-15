package com.uade.skillswap.application.ports.in.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.PublicationStatus;

public interface ListMyPublicationsUseCase {

    // Publicaciones del autor en todos sus estados (panel del proveedor). Con status, solo las de ese estado.
    Page<PublicationView> listMine(UserId authorId, PublicationStatus status, PageRequest page);
}
