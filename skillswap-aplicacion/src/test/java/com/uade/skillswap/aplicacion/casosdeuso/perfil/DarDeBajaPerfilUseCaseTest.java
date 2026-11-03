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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DarDeBajaPerfilUseCaseTest {

    @Mock
    private PerfilRepository perfilRepository;

    @InjectMocks
    private DarDeBajaPerfilUseCase useCase;

    @Test
    @DisplayName("Baja lógica: desactiva el perfil y lo persiste")
    void desactivaPerfil() {
        Perfil perfil = new Perfil("usr-1", "Lucas", "Bio", ZoneId.systemDefault(), TipoCuenta.PROFESIONAL);
        when(perfilRepository.buscarPorUserId("usr-1")).thenReturn(Optional.of(perfil));

        useCase.ejecutar("usr-1");

        assertThat(perfil.estaActivo()).isFalse();
        verify(perfilRepository).guardar(perfil);
    }

    @Test
    @DisplayName("Rechaza baja si el userId es nulo o en blanco")
    void rechazaUserIdInvalido() {
        assertThatThrownBy(() -> useCase.ejecutar(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ejecutar("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Lanza excepción si el perfil a dar de baja no existe")
    void rechazaSiNoExiste() {
        when(perfilRepository.buscarPorUserId("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.ejecutar("inexistente"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
