package com.uade.skillswap.aplicacion.casosdeuso.perfil;

import com.uade.skillswap.aplicacion.puertos.salida.PerfilRepository;
import com.uade.skillswap.dominio.perfil.Perfil;
import com.uade.skillswap.dominio.perfil.TipoCuenta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrearPerfilUseCaseTest {

    @Mock
    private PerfilRepository perfilRepository;

    private CrearPerfilUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CrearPerfilUseCase(perfilRepository);
    }

    @Test
    @DisplayName("Crea y persiste un nuevo perfil si el userId no existe previamente")
    void creaPerfilExitosamente() {
        when(perfilRepository.existePorUserId("usr-1")).thenReturn(false);
        when(perfilRepository.guardar(any(Perfil.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CrearPerfilUseCase.Command command = new CrearPerfilUseCase.Command(
                "usr-1",
                "Juan Pérez",
                "Ingeniero de software",
                ZoneId.of("America/Argentina/Buenos_Aires"),
                TipoCuenta.PROFESIONAL
        );

        Perfil resultado = useCase.ejecutar(command);

        assertThat(resultado.getUserId()).isEqualTo("usr-1");
        assertThat(resultado.getNombreVisible()).isEqualTo("Juan Pérez");
        assertThat(resultado.getTipoCuenta()).isEqualTo(TipoCuenta.PROFESIONAL);
        verify(perfilRepository).guardar(any(Perfil.class));
    }

    @Test
    @DisplayName("Rechaza creación si ya existe un perfil con ese userId")
    void rechazaSiYaExiste() {
        when(perfilRepository.existePorUserId("usr-1")).thenReturn(true);

        CrearPerfilUseCase.Command command = new CrearPerfilUseCase.Command(
                "usr-1",
                "Juan Pérez",
                "Bio",
                ZoneId.systemDefault(),
                TipoCuenta.PROFESIONAL
        );

        assertThatThrownBy(() -> useCase.ejecutar(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe un perfil");

        verify(perfilRepository, never()).guardar(any());
    }
}
