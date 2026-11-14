package com.uade.skillswap.application.ports.out.publication;

import com.uade.skillswap.domain.publication.Publication;

import java.util.Optional;

// Puerto de salida: lo implementa infraestructura con JPA.
public interface PublicationRepository {

    Optional<Publication> findById(long id);

    // Devuelve la publicación guardada (con id asignado si era nueva).
    Publication save(Publication publication);
}
