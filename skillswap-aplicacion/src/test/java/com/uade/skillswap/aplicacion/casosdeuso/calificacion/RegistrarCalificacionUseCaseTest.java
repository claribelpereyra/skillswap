package com.uade.skillswap.aplicacion.casosdeuso.calificacion;

import com.uade.skillswap.aplicacion.puertos.salida.CalificacionRepository;
import com.uade.skillswap.aplicacion.puertos.salida.ConsultaContratacionCalificable;
import com.uade.skillswap.aplicacion.puertos.salida.PerfilRepository;
import com.uade.skillswap.dominio.calificacion.Calificacion;
import com.uade.skillswap.dominio.calificacion.excepciones.CalificacionInvalidaException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrarCalificacionUseCaseTest {

    @Mock
    private CalificacionRepository calificacionRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private ConsultaContratacionCalificable consultaContratacion;

    @InjectMocks
    private RegistrarCalificacionUseCase useCase;

    private Perfil perfilDestinatario;

    @BeforeEach
    void setUp() {
        perfilDestinatario = new Perfil(
                "usr-proveedor",
                "Carlos Mentor",
                "Especialista",
                ZoneId.systemDefault(),
                TipoCuenta.PROFESIONAL
        );
    }

    @Test
    @DisplayName("Caso 10: Registra calificación verificada y recalcula la reputación del proveedor automáticamente")
    void registraCalificacionYRecalculaReputacionCaso10() {
        Long contratacionId = 100L;
        String autorId = "usr-cliente";
        String destinatarioId = "usr-proveedor";

        when(consultaContratacion.esContratacionCompletada(contratacionId)).thenReturn(true);
        when(consultaContratacion.participoEnContratacion(contratacionId, autorId)).thenReturn(true);
        when(calificacionRepository.yaCalifico(contratacionId, autorId, destinatarioId)).thenReturn(false);
        when(calificacionRepository.guardar(any(Calificacion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(perfilRepository.buscarPorUserId(destinatarioId)).thenReturn(Optional.of(perfilDestinatario));

        RegistrarCalificacionUseCase.Command command = new RegistrarCalificacionUseCase.Command(
                contratacionId,
                autorId,
                destinatarioId,
                5,
                "Excelente sesión de arquitectura"
        );

        Calificacion resultado = useCase.ejecutar(command);

        assertThat(resultado.getPuntaje()).isEqualTo(5);
        assertThat(resultado.getAutorId()).isEqualTo("usr-cliente");
        assertThat(resultado.getDestinatarioId()).isEqualTo("usr-proveedor");

        // Verificamos que la reputación del destinatario pasó de 0 a 5.0
        assertThat(perfilDestinatario.getReputacion()).isEqualTo(5.0);
        assertThat(perfilDestinatario.getCantidadCalificaciones()).isEqualTo(1);

        verify(calificacionRepository).guardar(any(Calificacion.class));
        verify(perfilRepository).guardar(perfilDestinatario);
    }

    @Test
    @DisplayName("Rechaza calificación si la contratación no está COMPLETADA")
    void rechazaSiContratacionNoCompletada() {
        when(consultaContratacion.esContratacionCompletada(100L)).thenReturn(false);

        RegistrarCalificacionUseCase.Command command = new RegistrarCalificacionUseCase.Command(
                100L, "usr-cliente", "usr-proveedor", 5, "Comentario"
        );

        assertThatThrownBy(() -> useCase.ejecutar(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETADA");

        verify(calificacionRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Rechaza calificación si el usuario no fue participante")
    void rechazaSiUsuarioNoParticipo() {
        when(consultaContratacion.esContratacionCompletada(100L)).thenReturn(true);
        when(consultaContratacion.participoEnContratacion(100L, "usr-intruso")).thenReturn(false);

        RegistrarCalificacionUseCase.Command command = new RegistrarCalificacionUseCase.Command(
                100L, "usr-intruso", "usr-proveedor", 5, "Comentario"
        );

        assertThatThrownBy(() -> useCase.ejecutar(command))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("no fue participante");

        verify(calificacionRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Rechaza calificación duplicada por parte del mismo participante")
    void rechazaCalificacionDuplicada() {
        when(consultaContratacion.esContratacionCompletada(100L)).thenReturn(true);
        when(consultaContratacion.participoEnContratacion(100L, "usr-cliente")).thenReturn(true);
        when(calificacionRepository.yaCalifico(100L, "usr-cliente", "usr-proveedor")).thenReturn(true);

        RegistrarCalificacionUseCase.Command command = new RegistrarCalificacionUseCase.Command(
                100L, "usr-cliente", "usr-proveedor", 5, "Comentario"
        );

        assertThatThrownBy(() -> useCase.ejecutar(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicada");

        verify(calificacionRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Rechaza intento de autocalificación")
    void rechazaAutocalificacion() {
        when(consultaContratacion.esContratacionCompletada(100L)).thenReturn(true);
        when(consultaContratacion.participoEnContratacion(100L, "usr-mismo")).thenReturn(true);
        when(calificacionRepository.yaCalifico(100L, "usr-mismo", "usr-mismo")).thenReturn(false);

        RegistrarCalificacionUseCase.Command command = new RegistrarCalificacionUseCase.Command(
                100L, "usr-mismo", "usr-mismo", 5, "Me autocalifico"
        );

        assertThatThrownBy(() -> useCase.ejecutar(command))
                .isInstanceOf(CalificacionInvalidaException.class)
                .hasMessageContaining("no puede calificarse a sí mismo");
    }
}
