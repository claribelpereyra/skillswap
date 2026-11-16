package com.uade.skillswap.infraestructura.configuracion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Duration;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ConfiguracionFuncionalTest {
    @Test void usaDefaultsDocumentadosYPermiteElegirOtraConfiguracion() {
        var defecto = ConfiguracionFuncional.desde(Map.of());
        assertThat(defecto.anticipacionReintegroTotal()).isEqualTo(Duration.ofHours(48));
        assertThat(defecto.anticipacionReintegroParcial()).isEqualTo(Duration.ofHours(24));
        assertThat(defecto.estrategiaRecomendacion()).isEqualTo(ConfiguracionFuncional.EstrategiaRecomendacion.AFINIDAD);
        var otra = ConfiguracionFuncional.desde(Map.of("SKILLSWAP_CANCELACION_TOTAL_HORAS", "72",
                "SKILLSWAP_CANCELACION_PARCIAL_HORAS", "12", "SKILLSWAP_RECOMENDACION", " popularidad "));
        assertThat(otra.anticipacionReintegroTotal()).isEqualTo(Duration.ofHours(72));
        assertThat(otra.anticipacionReintegroParcial()).isEqualTo(Duration.ofHours(12));
        assertThat(otra.estrategiaRecomendacion()).isEqualTo(ConfiguracionFuncional.EstrategiaRecomendacion.POPULARIDAD);
        assertThat(ConfiguracionFuncional.desde(Map.of("SKILLSWAP_RECOMENDACION", " "))).isEqualTo(defecto);
    }

    @ParameterizedTest @ValueSource(strings = {"texto", "1.5", "-1", "9223372036854775807"})
    void rechazaUmbralInvalido(String valor) {
        assertThatThrownBy(() -> ConfiguracionFuncional.desde(Map.of("SKILLSWAP_CANCELACION_PARCIAL_HORAS", valor)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rechazaUmbralesInvertidosIgualesYEstrategiaDesconocida() {
        assertThatThrownBy(() -> ConfiguracionFuncional.desde(Map.of("SKILLSWAP_CANCELACION_TOTAL_HORAS", "12")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConfiguracionFuncional.desde(Map.of("SKILLSWAP_CANCELACION_TOTAL_HORAS", "24")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConfiguracionFuncional.desde(Map.of("SKILLSWAP_RECOMENDACION", "azar")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
