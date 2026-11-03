package com.uade.skillswap.aplicacion.seguridad;

import com.uade.skillswap.aplicacion.puertos.salida.ServicioIdentidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorPermisosTest {

    @Mock
    private ServicioIdentidad servicioIdentidad;

    @InjectMocks
    private ValidadorPermisos validadorPermisos;

    @Test
    @DisplayName("Valida con éxito cuando el usuario activo coincide con el esperado")
    void validaPropietarioCorrecto() {
        when(servicioIdentidad.hayUsuarioAutenticado()).thenReturn(true);
        when(servicioIdentidad.obtenerUserIdActivo()).thenReturn("usr-andy");

        validadorPermisos.validarEsPropietario("usr-andy");
        assertThat(validadorPermisos.obtenerUserIdAutenticado()).isEqualTo("usr-andy");
    }

    @Test
    @DisplayName("Rechaza acceso si no hay sesión activa")
    void rechazaSinSesion() {
        when(servicioIdentidad.hayUsuarioAutenticado()).thenReturn(false);

        assertThatThrownBy(() -> validadorPermisos.validarEsPropietario("usr-andy"))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("No hay ninguna sesión activa");
    }

    @Test
    @DisplayName("Rechaza acceso si el usuario activo no coincide con el recurso a modificar")
    void rechazaUsuarioDiferente() {
        when(servicioIdentidad.hayUsuarioAutenticado()).thenReturn(true);
        when(servicioIdentidad.obtenerUserIdActivo()).thenReturn("usr-hacker");

        assertThatThrownBy(() -> validadorPermisos.validarEsPropietario("usr-victima"))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("no tiene permisos");
    }
}
