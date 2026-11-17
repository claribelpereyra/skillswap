package com.uade.skillswap.application.ports.out.publication;

import java.util.Optional;
import java.util.Set;

// Puerto de salida hacia el catálogo: ids de una categoría y todo lo que cuelga de ella.
public interface CategoryTree {

    Optional<Set<Long>> idsUnder(long categoryId);

    default boolean contains(long categoryId) {
        return idsUnder(categoryId).isPresent();
    }
}
