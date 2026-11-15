package com.uade.skillswap.application.dto.publication;

// Acciones que el autor puede pedir sobre su publicación. Cuál es válida la decide el estado del dominio.
public enum PublicationAction {
    PUBLISH,
    PAUSE,
    RESUME,
    CLOSE,
    CANCEL
}
