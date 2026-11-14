package com.uade.skillswap.domain.publication;

import com.uade.skillswap.domain.common.InvalidTransitionException;

// Patrón State: cada estado decide qué acciones admite y a qué estado lleva.
// Es package-private a propósito: solo Publication aplica la transición, así nadie de afuera
// puede forzar un estado salteando las reglas.
abstract class PublicationState {

    abstract PublicationStatus status();

    PublicationState publish(Publication publication) {
        throw invalid("publish");
    }

    PublicationState pause() {
        throw invalid("pause");
    }

    PublicationState resume() {
        throw invalid("resume");
    }

    PublicationState close() {
        throw invalid("close");
    }

    PublicationState cancel() {
        throw invalid("cancel");
    }

    void checkEditable() {
        throw invalid("edit");
    }

    // Recibe solicitudes nuevas.
    boolean acceptsRequests() {
        return false;
    }

    // La ven todos (búsqueda y detalle). Si no, solo la ve su autor.
    boolean isPublic() {
        return false;
    }

    // Admite ocupar o liberar lugares de solicitudes ya hechas.
    boolean allowsReservations() {
        return false;
    }

    InvalidTransitionException invalid(String action) {
        return new InvalidTransitionException(status(), action);
    }

    static PublicationState of(PublicationStatus status) {
        return switch (status) {
            case DRAFT -> new Draft();
            case PUBLISHED -> new Published();
            case PAUSED -> new Paused();
            case CLOSED -> new Closed();
            case CANCELLED -> new Cancelled();
        };
    }

    static final class Draft extends PublicationState {

        @Override
        PublicationStatus status() {
            return PublicationStatus.DRAFT;
        }

        @Override
        PublicationState publish(Publication publication) {
            publication.checkCompleteForPublishing();
            return new Published();
        }

        @Override
        PublicationState cancel() {
            return new Cancelled();
        }

        @Override
        void checkEditable() {
            // En borrador se puede editar todo.
        }
    }

    static final class Published extends PublicationState {

        @Override
        PublicationStatus status() {
            return PublicationStatus.PUBLISHED;
        }

        @Override
        PublicationState pause() {
            return new Paused();
        }

        @Override
        PublicationState close() {
            return new Closed();
        }

        @Override
        PublicationState cancel() {
            return new Cancelled();
        }

        @Override
        boolean acceptsRequests() {
            return true;
        }

        @Override
        boolean isPublic() {
            return true;
        }

        @Override
        boolean allowsReservations() {
            return true;
        }
    }

    static final class Paused extends PublicationState {

        @Override
        PublicationStatus status() {
            return PublicationStatus.PAUSED;
        }

        @Override
        PublicationState resume() {
            return new Published();
        }

        @Override
        PublicationState close() {
            return new Closed();
        }

        @Override
        PublicationState cancel() {
            return new Cancelled();
        }

        // Pausada no recibe solicitudes nuevas, pero el autor puede seguir aceptando las pendientes
        // y una cancelación libera su lugar.
        @Override
        boolean allowsReservations() {
            return true;
        }
    }

    static final class Closed extends PublicationState {

        @Override
        PublicationStatus status() {
            return PublicationStatus.CLOSED;
        }
    }

    static final class Cancelled extends PublicationState {

        @Override
        PublicationStatus status() {
            return PublicationStatus.CANCELLED;
        }
    }
}
