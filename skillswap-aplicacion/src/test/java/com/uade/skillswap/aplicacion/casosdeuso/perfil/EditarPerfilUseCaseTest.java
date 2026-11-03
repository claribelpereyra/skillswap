package com.uade.skillswap.aplicacion.casosdeuso.perfil;

import com.uade.skillswap.aplicacion.puertos.salida.PerfilRepository;
import com.uade.skillswap.dominio.perfil.Perfil;
import com.uade.skillswap.dominio.perfil.TipoCuenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EditarPerfilUseCaseTest {

    @Mock
    private PerfilRepository perfilRepository;

    @InjectMocks
    private EditarPerfilUseCase useCase;

    @Test
    @DisplayName("Edita datos del perfil correctamente cuando existe")
    void editaPerfilExitosamente() {
        Perfil perfilExistente = new Perfil("usr-1", "Nombre Viejo", "Bio Vieja", ZoneId.systemDefault(), TipoCuenta.PROFESIONAL);
        when(perfilRepository.buscarPorUserId("usr-1")).thenReturn(Optional.of(perfilExistente));
        when(perfilRepository.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EditarPerfilUseCase.Command command = new EditarPerfilUseCase.Command(
                "usr-1", "Nombre Nuevo", "Bio Nueva", ZoneId.of("UTC")
        );

        Perfil editado = useCase.ejecutar(command);

        assertThat(editado.getNombreVisible()).isEqualTo("Nombre Nuevo");
        assertThat(editado.getBiografia()).isEqualTo("Bio Nueva");
        assertThat(editado.getZonaHoraria()).isEqualTo(ZoneId.of("UTC"));
        verify(perfilRepository).guardar(perfilExistente);
    }

    @Test
    @DisplayName("Lanza excepción si el perfil no existe")
    void rechazaSiPerfilNoExiste() {
        when(perfilRepository.buscarPorUserId("inexistente")).thenReturn(Optional.empty());

        EditarPerfilUseCase.Command command = new EditarPerfilUseCase.Command(
                "inexistente", "Nombre", "Bio", ZoneId.systemDefault()
        );

        assertThatThrownBy(() -> useCase.ejecutar(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No existe perfil registrado");

        verify(perfilRepository, never()).guardar(any());
    }
}
