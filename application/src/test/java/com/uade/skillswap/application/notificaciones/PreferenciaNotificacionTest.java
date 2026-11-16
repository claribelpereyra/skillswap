package com.uade.skillswap.application.notificaciones;

import com.uade.skillswap.application.usecases.PreferenciaNotificacionCasoDeUso;
import com.uade.skillswap.application.ports.out.RepositorioPreferencias;
import com.uade.skillswap.domain.notificaciones.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PreferenciaNotificacionTest {
    @Test void usaInternaPorDefectoYRespetaLaPreferenciaGuardada() {
        var repo = mock(RepositorioPreferencias.class);
        when(repo.buscar("ana")).thenReturn(Optional.empty(),
                Optional.of(new PreferenciaNotificacion("ana", CanalPreferido.PUSH)));
        var caso = new PreferenciaNotificacionCasoDeUso(repo);
        assertThat(caso.consultar("ana").canal()).isEqualTo(CanalPreferido.INTERNA);
        caso.cambiar("ana", CanalPreferido.PUSH);
        verify(repo).guardar(new PreferenciaNotificacion("ana", CanalPreferido.PUSH));
        assertThat(caso.consultar("ana").canal()).isEqualTo(CanalPreferido.PUSH);
    }

    @Test void noGuardaPreferenciasInvalidas() {
        var repo = mock(RepositorioPreferencias.class);
        var caso = new PreferenciaNotificacionCasoDeUso(repo);
        assertThatThrownBy(() -> caso.cambiar(" ", CanalPreferido.EMAIL)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> caso.cambiar("ana", null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(repo);
    }
}
