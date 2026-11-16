package com.uade.skillswap.application.notificaciones;

import com.uade.skillswap.application.ports.out.CanalNotificacion;
import com.uade.skillswap.domain.notificaciones.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FabricaCanalTest {
    @Test void seleccionaCadaCanalSinCambiarElConsumidor() {
        var adaptadores = Arrays.stream(CanalPreferido.values()).map(tipo -> {
            var canal = mock(CanalNotificacion.class);
            when(canal.tipo()).thenReturn(tipo);
            return canal;
        }).toList();
        var fabrica = new FabricaCanalNotificacion(adaptadores);
        for (var canal : adaptadores) assertThat(fabrica.crear(canal.tipo())).isSameAs(canal);
        assertThatThrownBy(() -> new FabricaCanalNotificacion(List.of(adaptadores.get(0))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FabricaCanalNotificacion(List.of(adaptadores.get(0), adaptadores.get(0))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
