package com.uade.skillswap.controllers.comun;

import com.uade.skillswap.application.notificaciones.NotificacionNoEncontrada;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ResultadoControllerTest {
    @Test void traduceErroresConFormatoComunSinExponerDetallesTecnicos() {
        assertThat(EjecutorController.ejecutar(() -> "ok").datos()).isEqualTo("ok");
        assertThat(EjecutorController.ejecutar(() -> { throw new IllegalArgumentException("dato"); }).error().codigo())
                .isEqualTo(CodigoError.VALIDACION);
        assertThat(EjecutorController.ejecutar(() -> { throw new NotificacionNoEncontrada(); }).error().codigo())
                .isEqualTo(CodigoError.NO_ENCONTRADO);
        assertThat(EjecutorController.ejecutar(() -> { throw new AccesoNoAutorizado(); }).error().codigo())
                .isEqualTo(CodigoError.NO_AUTORIZADO);
        var fallo = EjecutorController.ejecutar(() -> { throw new IllegalStateException("SQL password=secreto"); });
        assertThat(fallo.exitoso()).isFalse();
        assertThat(fallo.error().codigo()).isEqualTo(CodigoError.INTERNO);
        assertThat(fallo.error().mensaje()).doesNotContain("SQL", "secreto");
    }

    @Test void impideResultadosAmbiguos() {
        assertThatThrownBy(() -> new Resultado<>(null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Resultado<>("dato", new ErrorController(CodigoError.INTERNO, "falló")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
