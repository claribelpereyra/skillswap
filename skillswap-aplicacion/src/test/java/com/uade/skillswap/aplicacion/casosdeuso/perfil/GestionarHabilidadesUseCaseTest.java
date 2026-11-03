package com.uade.skillswap.aplicacion.casosdeuso.perfil;

import com.uade.skillswap.aplicacion.puertos.salida.PerfilRepository;
import com.uade.skillswap.dominio.perfil.Habilidad;
import com.uade.skillswap.dominio.perfil.Perfil;
import com.uade.skillswap.dominio.perfil.TipoCuenta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarHabilidadesUseCaseTest {

    @Mock
    private PerfilRepository perfilRepository;

    @InjectMocks
    private GestionarHabilidadesUseCase useCase;

    private Perfil perfil;

    @BeforeEach
    void setUp() {
        perfil = new Perfil("usr-1", "Andy", "Bio", ZoneId.systemDefault(), TipoCuenta.PROFESIONAL);
        when(perfilRepository.buscarPorUserId("usr-1")).thenReturn(Optional.of(perfil));
        when(perfilRepository.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Agrega y quita habilidades del perfil")
    void gestionaHabilidades() {
        useCase.agregarHabilidad("usr-1", "Java");
        assertThat(perfil.getHabilidades()).contains(new Habilidad("Java"));

        useCase.quitarHabilidad("usr-1", "Java");
        assertThat(perfil.getHabilidades()).isEmpty();

        verify(perfilRepository, org.mockito.Mockito.times(2)).guardar(perfil);
    }

    @Test
    @DisplayName("Agrega y quita intereses del perfil")
    void gestionaIntereses() {
        useCase.agregarInteres("usr-1", "Arquitectura");
        assertThat(perfil.getIntereses()).contains("Arquitectura");

        useCase.quitarInteres("usr-1", "Arquitectura");
        assertThat(perfil.getIntereses()).isEmpty();

        verify(perfilRepository, org.mockito.Mockito.times(2)).guardar(perfil);
    }
}
