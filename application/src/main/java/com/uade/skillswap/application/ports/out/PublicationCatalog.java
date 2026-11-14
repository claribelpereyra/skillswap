package com.uade.skillswap.application.ports.out;

import java.util.Optional;

public interface PublicationCatalog {

    Optional<BookablePublication> findBookable(long publicationId);
}
