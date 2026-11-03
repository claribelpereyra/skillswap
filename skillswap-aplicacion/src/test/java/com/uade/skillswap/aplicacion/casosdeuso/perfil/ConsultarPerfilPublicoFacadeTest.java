package com.uade.skillswap.aplicacion.casosdeuso.perfil;

import com.uade.skillswap.aplicacion.casosdeuso.perfil.ConsultarPerfilPublicoFacade.PerfilPublicoDto;
import com.uade.skillswap.aplicacion.puertos.salida.ConsultaPublicacionesActivas;
import com.uade.skillswap.aplicacion.puertos.salida.ConsultaPublicacionesActivas.PublicacionResumenDto;
import com.uade.skillswap.aplicacion.puertos.salida.PerfilRepository;
import com.uade.skillswap.dominio.perfil.Habilidad;
import com.uade.skillswap.dominio.perfil.Perfil;
import com.uade.skillswap.dominio.perfil.TipoCuenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarPerfilPublicoFacadeTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ConsultaPublicacionesActivas consultaPublicaciones;

    @InjectMocks
    private ConsultarPerfilPublicoFacade facade;

    @Test
    @DisplayName("Patrón Facade: unifica perfil, reputación y publicaciones activas en una sola llamada")
    void consultaPerfilPublicoConsolidado() {
        Perfil perfil = new Perfil("usr-pro", "Ana Profesora", "Mentora experta", ZoneId.systemDefault(), TipoCuenta.PROFESIONAL);
        perfil.agregarHabilidad(new Habilidad("Java"));
        perfil.registrarCalificacion(5);

        List<PublicacionResumenDto> publicaciones = List.of(
                new PublicacionResumenDto(1L, "Mentoría Java Avanzado", "MENTORIA", 100),
                new PublicacionResumenDto(2L, "Consultoría Arquitectura", "SERVICIO", 250)
        );

        when(perfilRepository.buscarPorUserId("usr-pro")).thenReturn(Optional.of(perfil));
        when(consultaPublicaciones.obtenerPublicacionesActivasDeAutor("usr-pro")).thenReturn(publicaciones);

        PerfilPublicoDto dto = facade.consultar("usr-pro");

        assertThat(dto.userId()).isEqualTo("usr-pro");
        assertThat(dto.nombreVisible()).isEqualTo("Ana Profesora");
        assertThat(dto.reputacion()).isEqualTo(5.0);
        assertThat(dto.cantidadCalificaciones()).isEqualTo(1);
        assertThat(dto.habilidades()).contains(new Habilidad("Java"));
        assertThat(dto.publicacionesActivas()).hasSize(2);
    }

    @Test
    @DisplayName("Lanza excepción si el perfil solicitado no existe")
    void perfilNoEncontrado() {
        when(perfilRepository.buscarPorUserId("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facade.consultar("inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Perfil no encontrado");
    }
}
