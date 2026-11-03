package com.uade.skillswap.dominio.calificacion;

import com.uade.skillswap.dominio.calificacion.excepciones.CalificacionInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalificacionTest {

    @Test
    @DisplayName("Crea calificación verificada válida")
    void creaCalificacionValida() {
        Calificacion calificacion = new Calificacion(
                10L,
                "usr-cliente",
                "usr-proveedor",
                5,
                "Excelente mentoría y puntualidad"
        );

        assertThat(calificacion.getContratacionId()).isEqualTo(10L);
        assertThat(calificacion.getAutorId()).isEqualTo("usr-cliente");
        assertThat(calificacion.getDestinatarioId()).isEqualTo("usr-proveedor");
        assertThat(calificacion.getPuntaje()).isEqualTo(5);
        assertThat(calificacion.getComentario()).isEqualTo("Excelente mentoría y puntualidad");
        assertThat(calificacion.getFecha()).isNotNull();
    }

    @Test
    @DisplayName("Test Negativo Obligatorio: Rechaza intento de autocalificación (autorId == destinatarioId)")
    void noPermiteCalificarseASiMismo() {
        assertThatThrownBy(() -> new Calificacion(
                10L,
                "usr-andy",
                "usr-andy",
                5,
                "Me auto-califico con 5"
        ))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("no puede calificarse a sí mismo");
    }

    @Test
    @DisplayName("Test Negativo: Rechaza puntaje menor a 1")
    void noPermitePuntajeMenorAUno() {
        assertThatThrownBy(() -> new Calificacion(10L, "usr-c", "usr-p", 0, "Malo"))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("entre 1 y 5");
    }

    @Test
    @DisplayName("Test Negativo: Rechaza puntaje mayor a 5")
    void noPermitePuntajeMayorACinco() {
        assertThatThrownBy(() -> new Calificacion(10L, "usr-c", "usr-p", 6, "Excelente+"))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("entre 1 y 5");
    }

    @Test
    @DisplayName("Test Negativo: Rechaza contratación nula")
    void noPermiteContratacionNula() {
        assertThatThrownBy(() -> new Calificacion(null, "usr-c", "usr-p", 5, "Buen servicio"))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("asociada a una contratación");
    }

    @Test
    @DisplayName("Test Negativo: Rechaza autor nulo o vacío")
    void noPermiteAutorInvalido() {
        assertThatThrownBy(() -> new Calificacion(10L, "", "usr-p", 5, "Comentario"))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("autor");
    }

    @Test
    @DisplayName("Test Negativo: Rechaza destinatario nulo o vacío")
    void noPermiteDestinatarioInvalido() {
        assertThatThrownBy(() -> new Calificacion(10L, "usr-c", null, 5, "Comentario"))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("destinatario");
    }
}
