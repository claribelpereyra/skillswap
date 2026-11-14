package com.uade.skillswap.controllers.common;

public enum ErrorCode {
    // Datos de entrada mal formados (falta un campo, un valor fuera de rango, un enum desconocido).
    INVALID_INPUT,
    // No existe, o el usuario no lo puede ver.
    NOT_FOUND,
    // El usuario no tiene permiso para esa operación.
    FORBIDDEN,
    // La acción no es válida en el estado actual (patrón State).
    INVALID_TRANSITION,
    // Se violó una regla de negocio (oferta incompleta, cupo agotado, autor que se solicita a sí mismo...).
    BUSINESS_RULE,
    // Error no previsto: se informa genérico y no se filtran detalles internos.
    UNEXPECTED
}
