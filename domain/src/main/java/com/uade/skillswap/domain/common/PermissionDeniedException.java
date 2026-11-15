package com.uade.skillswap.domain.common;

// El usuario no puede hacer esa acción sobre ese objeto (por ejemplo, editar una publicación ajena).
// Es una regla de negocio, pero separada para que la interfaz pueda mostrarla como "no tenés permiso".
public class PermissionDeniedException extends BusinessRuleException {

    public PermissionDeniedException(String message) {
        super(message);
    }
}
