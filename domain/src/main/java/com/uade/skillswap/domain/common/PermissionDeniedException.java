package com.uade.skillswap.domain.common;

// El usuario no tiene permiso para esa acción (por ejemplo, editar una publicación ajena).
public class PermissionDeniedException extends BusinessRuleException {

    public PermissionDeniedException(String message) {
        super(message);
    }
}
