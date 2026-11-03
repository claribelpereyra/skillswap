package com.uade.skillswap.aplicacion.casosdeuso.calificacion;

import com.uade.skillswap.aplicacion.puertos.salida.CalificacionRepository;
import com.uade.skillswap.aplicacion.puertos.salida.ConsultaContratacionCalificable;
import com.uade.skillswap.aplicacion.puertos.salida.PerfilRepository;
import com.uade.skillswap.dominio.calificacion.Calificacion;
import com.uade.skillswap.dominio.perfil.Perfil;

import java.util.Objects;

/**
 * Caso de uso: Registrar una calificación verificada y recalcular automáticamente la reputación (§4.4, §5).
 * Implementa el Criterio de Aceptación 10 del TP.
 * <p>
 * Reglas de negocio obligatorias:
 * 1. La contratación debe encontrarse en estado COMPLETADA.
 * 2. El autor debe haber sido participante verificado de la contratación.
 * 3. Restricción estricta de una sola calificación por participante y contratación.
 * 4. Nadie se califica a sí mismo.
 * 5. La reputación del destinatario se recalcula de inmediato como promedio ponderado.
 */
public class RegistrarCalificacionUseCase {

    public record Command(
            Long contratacionId,
            String autorId,
            String destinatarioId,
            int puntaje,
            String comentario
    ) {}

    private final CalificacionRepository calificacionRepository;
    private final PerfilRepository perfilRepository;
    private final ConsultaContratacionCalificable consultaContratacion;

    public RegistrarCalificacionUseCase(CalificacionRepository calificacionRepository,
                                        PerfilRepository perfilRepository,
                                        ConsultaContratacionCalificable consultaContratacion) {
        this.calificacionRepository = Objects.requireNonNull(calificacionRepository, "El repositorio de calificaciones no puede ser nulo.");
        this.perfilRepository = Objects.requireNonNull(perfilRepository, "El repositorio de perfiles no puede ser nulo.");
        this.consultaContratacion = Objects.requireNonNull(consultaContratacion, "La consulta de contratación no puede ser nula.");
    }

    public Calificacion ejecutar(Command command) {
        Objects.requireNonNull(command, "El comando no puede ser nulo.");

        // 1. Verificación del estado de la contratación
        if (!consultaContratacion.esContratacionCompletada(command.contratacionId())) {
            throw new IllegalStateException("Solo se pueden emitir calificaciones para contrataciones en estado COMPLETADA.");
        }

        // 2. Verificación de participación legítima
        if (!consultaContratacion.participoEnContratacion(command.contratacionId(), command.autorId())) {
            throw new SecurityException("Calificación no autorizada: El usuario '" + command.autorId() +
                    "' no fue participante de la contratación #" + command.contratacionId());
        }

        // 3. Regla de unicidad estricta (una sola calificación por parte)
        if (calificacionRepository.yaCalifico(command.contratacionId(), command.autorId(), command.destinatarioId())) {
            throw new IllegalStateException("Calificación duplicada: Ya se ha registrado una calificación de '" +
                    command.autorId() + "' para esta contratación.");
        }

        // 4. Instanciación en dominio (valida invariantes: rango 1..5 y no autocalificarse)
        Calificacion nuevaCalificacion = new Calificacion(
                command.contratacionId(),
                command.autorId(),
                command.destinatarioId(),
                command.puntaje(),
                command.comentario()
        );

        // 5. Persistencia de la calificación verificada
        Calificacion guardada = calificacionRepository.guardar(nuevaCalificacion);

        // 6. Recálculo automático de reputación del usuario calificado (§4.1, Caso 10)
        Perfil perfilDestinatario = perfilRepository.buscarPorUserId(command.destinatarioId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el perfil del destinatario: " + command.destinatarioId()));

        perfilDestinatario.registrarCalificacion(command.puntaje());
        perfilRepository.guardar(perfilDestinatario);

        return guardada;
    }
}
