package com.uade.skillswap.application.ports.out.publication;

import java.util.Optional;
import java.util.Set;

// Puerto de salida hacia el catálogo de categorías. Infraestructura lo implementa armando el Composite
// del dominio (Category/Skill) y pidiéndole idsInSubtree() al nodo.
public interface CategoryTree {

    // La categoría y todo lo que cuelga de ella. Vacío si la categoría no existe.
    Optional<Set<Long>> idsUnder(long categoryId);
}
