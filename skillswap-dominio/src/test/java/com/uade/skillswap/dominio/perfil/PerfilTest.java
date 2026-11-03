package com.uade.skillswap.dominio.perfil;

import com.uade.skillswap.dominio.perfil.excepciones.PerfilInactivoException;
import com.uade.skillswap.dominio.perfil.excepciones.PerfilInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerfilTest {

    private Perfil perfil;

    @BeforeEach
    void setUp() {
        perfil = new Perfil(
                "usr-andy",
                "Andy Desarrollador",
                "Especialista en backend Java",
                ZoneId.of("America/Argentina/Buenos_Aires"),
                TipoCuenta.PROFESIONAL
        );
    }

    @Test
    @DisplayName("Crea perfil con datos válidos en estado activo y reputación inicial cero")
    void creaPerfilConDatosValidos() {
        assertThat(perfil.getUserId()).isEqualTo("usr-andy");
        assertThat(perfil.getNombreVisible()).isEqualTo("Andy Desarrollador");
        assertThat(perfil.getBiografia()).isEqualTo("Especialista en backend Java");
        assertThat(perfil.getTipoCuenta()).isEqualTo(TipoCuenta.PROFESIONAL);
        assertThat(perfil.estaActivo()).isTrue();
        assertThat(perfil.getReputacion()).isEqualTo(0.0);
        assertThat(perfil.getCantidadCalificaciones()).isEqualTo(0);
        assertThat(perfil.getHabilidades()).isEmpty();
    }

    @Test
    @DisplayName("Rechaza creación de perfil con nombre visible nulo o en blanco")
    void noPermiteCrearPerfilConNombreInvalido() {
        assertThatThrownBy(() -> new Perfil("usr-1", null, "Bio", ZoneId.systemDefault(), TipoCuenta.PROFESIONAL))
                .isInstanceOf(PerfilInvalidoException.class)
                .hasMessageContaining("nombre visible");

        assertThatThrownBy(() -> new Perfil("usr-1", "   ", "Bio", ZoneId.systemDefault(), TipoCuenta.PROFESIONAL))
                .isInstanceOf(PerfilInvalidoException.class)
                .hasMessageContaining("nombre visible");
    }

    @Test
    @DisplayName("Rechaza creación de perfil con userId nulo o vacío")
    void noPermiteCrearPerfilConUserIdInvalido() {
        assertThatThrownBy(() -> new Perfil("", "Lucas", "Bio", ZoneId.systemDefault(), TipoCuenta.PROFESIONAL))
                .isInstanceOf(PerfilInvalidoException.class)
                .hasMessageContaining("userId");
    }

    @Test
    @DisplayName("Rechaza creación de perfil sin tipo de cuenta")
    void noPermiteCrearPerfilSinTipoCuenta() {
        assertThatThrownBy(() -> new Perfil("usr-1", "Lucas", "Bio", ZoneId.systemDefault(), null))
                .isInstanceOf(PerfilInvalidoException.class)
                .hasMessageContaining("tipo de cuenta");
    }

    @Test
    @DisplayName("Agrega y quita habilidades garantizando unicidad insensible a mayúsculas")
    void gestionaHabilidadesSinDuplicados() {
        Habilidad java1 = new Habilidad("Java");
        Habilidad java2 = new Habilidad("java");
        Habilidad docker = new Habilidad("Docker");

        perfil.agregarHabilidad(java1);
        perfil.agregarHabilidad(java2); // Duplicado semántico
        perfil.agregarHabilidad(docker);

        assertThat(perfil.getHabilidades()).hasSize(2);

        perfil.quitarHabilidad(new Habilidad("Java"));
        assertThat(perfil.getHabilidades()).hasSize(1).contains(docker);
    }

    @Test
    @DisplayName("Agrega y remueve intereses del perfil")
    void gestionaIntereses() {
        perfil.agregarInteres("Arquitectura");
        perfil.agregarInteres("Diseño Orientado a Objetos");

        assertThat(perfil.getIntereses()).containsExactlyInAnyOrder("Arquitectura", "Diseño Orientado a Objetos");

        perfil.quitarInteres("Arquitectura");
        assertThat(perfil.getIntereses()).containsExactly("Diseño Orientado a Objetos");
    }

    @Test
    @DisplayName("Actualiza datos del perfil correctamente")
    void actualizaDatosPerfil() {
        perfil.actualizarDatos("Andy Senior", "Nueva biografía", ZoneId.of("UTC"));

        assertThat(perfil.getNombreVisible()).isEqualTo("Andy Senior");
        assertThat(perfil.getBiografia()).isEqualTo("Nueva biografía");
        assertThat(perfil.getZonaHoraria()).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    @DisplayName("Baja lógica: desactivar perfil impide modificaciones y preserva estado")
    void bajaLogicaDesactivaPerfil() {
        perfil.desactivar();
        assertThat(perfil.estaActivo()).isFalse();

        assertThatThrownBy(() -> perfil.agregarHabilidad(new Habilidad("Kotlin")))
                .isInstanceOf(PerfilInactivoException.class)
                .hasMessageContaining("perfil está inactivo");

        assertThatThrownBy(() -> perfil.actualizarDatos("Nuevo", "Bio", ZoneId.systemDefault()))
                .isInstanceOf(PerfilInactivoException.class);

        // Reactivación
        perfil.reactivar();
        assertThat(perfil.estaActivo()).isTrue();
        perfil.agregarHabilidad(new Habilidad("Kotlin"));
        assertThat(perfil.getHabilidades()).hasSize(1);
    }

    @Test
    @DisplayName("Caso 10: Recalcula la reputación automáticamente como promedio ponderado")
    void recalculaReputacionPonderadaCaso10() {
        // Primera calificación: 5 -> promedio 5.0
        perfil.registrarCalificacion(5);
        assertThat(perfil.getReputacion()).isEqualTo(5.0);
        assertThat(perfil.getCantidadCalificaciones()).isEqualTo(1);

        // Segunda calificación: 4 -> promedio (5 + 4) / 2 = 4.5
        perfil.registrarCalificacion(4);
        assertThat(perfil.getReputacion()).isEqualTo(4.5);
        assertThat(perfil.getCantidadCalificaciones()).isEqualTo(2);

        // Tercera calificación: 3 -> promedio (5 + 4 + 3) / 3 = 4.0
        perfil.registrarCalificacion(3);
        assertThat(perfil.getReputacion()).isEqualTo(4.0);
        assertThat(perfil.getCantidadCalificaciones()).isEqualTo(3);

        // Cuarta calificación: 5 -> promedio (12 + 5) / 4 = 4.25
        perfil.registrarCalificacion(5);
        assertThat(perfil.getReputacion()).isEqualTo(4.25);
        assertThat(perfil.getCantidadCalificaciones()).isEqualTo(4);
    }

    @Test
    @DisplayName("Rechaza registro de calificación con puntaje inválido")
    void rechazaCalificacionFueraDeRango() {
        assertThatThrownBy(() -> perfil.registrarCalificacion(0))
                .isInstanceOf(PerfilInvalidoException.class);

        assertThatThrownBy(() -> perfil.registrarCalificacion(6))
                .isInstanceOf(PerfilInvalidoException.class);
    }

    @Test
    @DisplayName("Genera proyección de resumen público para el patrón Facade")
    void generaResumenPerfilPublicoFacade() {
        perfil.agregarHabilidad(new Habilidad("Java"));
        perfil.registrarCalificacion(5);

        ResumenPerfilPublico resumen = perfil.obtenerResumenPublico();

        assertThat(resumen.userId()).isEqualTo("usr-andy");
        assertThat(resumen.nombreVisible()).isEqualTo("Andy Desarrollador");
        assertThat(resumen.tipoCuenta()).isEqualTo(TipoCuenta.PROFESIONAL);
        assertThat(resumen.reputacion()).isEqualTo(5.0);
        assertThat(resumen.cantidadCalificaciones()).isEqualTo(1);
        assertThat(resumen.habilidades()).contains(new Habilidad("Java"));
    }
}
