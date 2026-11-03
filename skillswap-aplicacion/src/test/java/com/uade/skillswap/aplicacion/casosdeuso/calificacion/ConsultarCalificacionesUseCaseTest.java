package com.uade.skillswap.aplicacion.casosdeuso.calificacion;

import com.uade.skillswap.aplicacion.puertos.salida.CalificacionRepository;
import com.uade.skillswap.dominio.calificacion.Calificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarCalificacionesUseCaseTest {

    @Mock
    private CalificacionRepository calificacionRepository;

    @InjectMocks
    private ConsultarCalificacionesUseCase useCase;

    @Test
    @DisplayName("Consulta calificaciones por destinatario correctamente")
    void listaCalificaciones() {
        List<Calificacion> mockList = List.of(
                new Calificacion(1L, "usr-a", "usr-dest", 5, "Genial"),
                new Calificacion(2L, "usr-b", "usr-dest", 4, "Muy bueno")
        );
        when(calificacionRepository.buscarPorDestinatario("usr-dest")).thenReturn(mockList);

        List<Calificacion> resultado = useCase.listarPorDestinatario("usr-dest");

        assertThat(resultado).hasSize(2);
        verify(calificacionRepository).buscarPorDestinatario("usr-dest");
    }

    @Test
    @DisplayName("Rechaza consulta con destinatario nulo o vacío")
    void rechazaDestinatarioInvalido() {
        assertThatThrownBy(() -> useCase.listarPorDestinatario(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.listarPorDestinatario("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
