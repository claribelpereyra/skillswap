package com.uade.skillswap.application.ports.out;

// Oculta cómo se cuenta el cupo (hoy por publicación, mañana por franja para sesiones grupales).
public interface PublicationAvailability {

    // Lanza BusinessRuleException si la franja está ocupada o no queda cupo.
    void reserve(long publicationId, long slotId);

    void release(long publicationId, long slotId);
}
