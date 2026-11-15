package com.uade.skillswap.domain.publication;

import com.uade.skillswap.domain.common.BusinessRuleException;

import java.util.List;

// Se lanza al intentar publicar una oferta a la que le faltan datos obligatorios (caso de aceptación 2).
public class IncompletePublicationException extends BusinessRuleException {

    private final List<String> missing;

    public IncompletePublicationException(List<String> missing) {
        super("The publication cannot be published, missing: " + String.join(", ", missing));
        this.missing = List.copyOf(missing);
    }

    public List<String> getMissing() {
        return missing;
    }
}
