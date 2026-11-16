package com.uade.skillswap.controllers.comun;

import com.uade.skillswap.aplicacion.notificaciones.NotificacionNoEncontrada;
import java.util.function.Supplier;

/** Traducción común de errores conocidos. Ampliar con los errores base cuando se integren. */
public final class EjecutorController {
    private static final System.Logger LOG = System.getLogger(EjecutorController.class.getName());
    private EjecutorController() {}

    public static <T> Resultado<T> ejecutar(Supplier<T> accion) {
        try {
            return Resultado.exito(accion.get());
        } catch (AccesoNoAutorizado error) {
            return Resultado.fallo(CodigoError.NO_AUTORIZADO, "Se requiere un usuario activo");
        } catch (NotificacionNoEncontrada error) {
            return Resultado.fallo(CodigoError.NO_ENCONTRADO, "No se encontró la notificación del usuario");
        } catch (IllegalArgumentException error) {
            return Resultado.fallo(CodigoError.VALIDACION, "Revisá los datos ingresados");
        } catch (RuntimeException error) {
            // No devolver mensajes de SQL, credenciales ni detalles internos a la UI.
            LOG.log(System.Logger.Level.ERROR, "Fallo en controller: {0}", error.getClass().getName());
            return Resultado.fallo(CodigoError.INTERNO, "No se pudo completar la operación");
        }
    }
}
