package com.uade.skillswap.infrastructure.configuracion;

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

    @Test void leePromocionesPorProveedorEnElOrdenConfiguradoYSinVariableNoHayNinguna() {
        var configuracion = ConfiguracionFuncional.desde(Map.of("SKILLSWAP_PROMOCIONES",
                " user-sofia = porcentaje:10 , FIJO:5 ; user-mateo=FIJO:20 "));

        assertThat(configuracion.promocionesPorProveedor()).containsOnlyKeys("user-sofia", "user-mateo");
        assertThat(configuracion.promocionesPorProveedor().get("user-sofia")).containsExactly(
                new ConfiguracionFuncional.PromocionConfigurada(ConfiguracionFuncional.TipoPromocion.PORCENTAJE, 10),
                new ConfiguracionFuncional.PromocionConfigurada(ConfiguracionFuncional.TipoPromocion.FIJO, 5));
        assertThat(configuracion.promocionesPorProveedor().get("user-mateo")).containsExactly(
                new ConfiguracionFuncional.PromocionConfigurada(ConfiguracionFuncional.TipoPromocion.FIJO, 20));
        assertThat(ConfiguracionFuncional.desde(Map.of()).promocionesPorProveedor()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"user-sofia", "=FIJO:5", "user-sofia=", "user-sofia=DESCUENTO:5", "user-sofia=FIJO",
            "user-sofia=FIJO:diez", "user-sofia=PORCENTAJE:101", "user-sofia=FIJO:-1", "user-sofia=FIJO:5,",
            "user-sofia=FIJO:5;user-sofia=FIJO:1"})
    void rechazaPromocionesMalFormadasFueraDeRangoORepetidas(String valor) {
        assertThatThrownBy(() -> ConfiguracionFuncional.desde(Map.of("SKILLSWAP_PROMOCIONES", valor)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKILLSWAP_PROMOCIONES");
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
