package com.uade.skillswap.infraestructura.adaptadores;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockServicioIdentidadAdapterTest {

    @Test
    @DisplayName("Inicializa sin usuario y rechaza obtener userId cuando no hay sesión")
    void inicializaSinUsuario() {
        MockServicioIdentidadAdapter adapter = new MockServicioIdentidadAdapter();

        assertThat(adapter.hayUsuarioAutenticado()).isFalse();
        assertThat(adapter.getUserIdActivoOpcional()).isEmpty();
        assertThatThrownBy(adapter::obtenerUserIdActivo)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay ningún usuario autenticado");
    }

    @Test
    @DisplayName("Simula login y logout correctamente")
    void simulaLoginYLogout() {
        MockServicioIdentidadAdapter adapter = new MockServicioIdentidadAdapter();

        adapter.simularLogin("usr-andy");
        assertThat(adapter.hayUsuarioAutenticado()).isTrue();
        assertThat(adapter.obtenerUserIdActivo()).isEqualTo("usr-andy");
        assertThat(adapter.getUserIdActivoOpcional()).contains("usr-andy");

        adapter.simularLogout();
        assertThat(adapter.hayUsuarioAutenticado()).isFalse();
        assertThat(adapter.getUserIdActivoOpcional()).isEmpty();
    }

    @Test
    @DisplayName("Rechaza simular login con identificador nulo o vacío")
    void rechazaLoginInvalido() {
        MockServicioIdentidadAdapter adapter = new MockServicioIdentidadAdapter();

        assertThatThrownBy(() -> adapter.simularLogin(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.simularLogin("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
