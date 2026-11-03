package com.uade.skillswap.dominio.calificacion;

import com.uade.skillswap.dominio.calificacion.excepciones.CalificacionInvalidaException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad de dominio que representa una calificación verificada emitida entre
 * participantes de una contratación completada.
 * <p>
 * Reglas de negocio obligatorias (§4.4, §5):
 * - El puntaje debe ser un entero entre 1 y 5 (ambos inclusive).
 * - Está prohibido que un usuario se califique a sí mismo (autorId != destinatarioId).
 * - La calificación debe asociarse a una contratación válida.
 */
public class Calificacion {

    private Long id;
    private final Long contratacionId;
    private final String autorId;
    private final String destinatarioId;
    private final int puntaje;
    private final String comentario;
    private final LocalDateTime fecha;

    public Calificacion(Long id,
                        Long contratacionId,
                        String autorId,
                        String destinatarioId,
                        int puntaje,
                        String comentario,
                        LocalDateTime fecha) {
        validarInvariantes(contratacionId, autorId, destinatarioId, puntaje);
        this.id = id;
        this.contratacionId = contratacionId;
        this.autorId = autorId.trim();
        this.destinatarioId = destinatarioId.trim();
        this.puntaje = puntaje;
        this.comentario = comentario != null ? comentario.trim() : "";
        this.fecha = fecha != null ? fecha : LocalDateTime.now();
    }

    public Calificacion(Long contratacionId,
                        String autorId,
                        String destinatarioId,
                        int puntaje,
                        String comentario) {
        this(null, contratacionId, autorId, destinatarioId, puntaje, comentario, LocalDateTime.now());
    }

    private void validarInvariantes(Long contratacionId, String autorId, String destinatarioId, int puntaje) {
        if (contratacionId == null) {
            throw new CalificacionInvalidaException("La calificación debe estar asociada a una contratación.");
        }
        if (autorId == null || autorId.trim().isEmpty()) {
            throw new CalificacionInvalidaException("El autor de la calificación no puede ser nulo ni vacío.");
        }
        if (destinatarioId == null || destinatarioId.trim().isEmpty()) {
            throw new CalificacionInvalidaException("El destinatario de la calificación no puede ser nulo ni vacío.");
        }
        if (autorId.trim().equalsIgnoreCase(destinatarioId.trim())) {
            throw new CalificacionInvalidaException("Un usuario no puede calificarse a sí mismo.");
        }
        if (puntaje < 1 || puntaje > 5) {
            throw new CalificacionInvalidaException("El puntaje debe estar comprendido entre 1 y 5 (recibido: " + puntaje + ").");
        }
    }

    // --- Getters de Acceso ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContratacionId() {
        return contratacionId;
    }

    public String getAutorId() {
        return autorId;
    }

    public String getDestinatarioId() {
        return destinatarioId;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Calificacion that = (Calificacion) o;
        return Objects.equals(contratacionId, that.contratacionId) &&
                Objects.equals(autorId, that.autorId) &&
                Objects.equals(destinatarioId, that.destinatarioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contratacionId, autorId, destinatarioId);
    }
}
