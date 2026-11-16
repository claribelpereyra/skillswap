package com.uade.skillswap.application.notificaciones;

import com.uade.skillswap.application.ports.out.CanalNotificacion;
import com.uade.skillswap.domain.notificaciones.CanalPreferido;
import java.util.*;

/** Simple Factory: devuelve el adaptador configurado para el canal solicitado. */
public final class FabricaCanalNotificacion {
    private final Map<CanalPreferido, CanalNotificacion> canales;

    public FabricaCanalNotificacion(Collection<CanalNotificacion> adaptadores) {
        var mapa = new EnumMap<CanalPreferido, CanalNotificacion>(CanalPreferido.class);
        for (var adaptador : adaptadores) {
            var tipo = Objects.requireNonNull(adaptador.tipo());
            if (mapa.putIfAbsent(tipo, adaptador) != null) throw new IllegalArgumentException("Canal duplicado: " + tipo);
        }
        if (mapa.size() != CanalPreferido.values().length) throw new IllegalArgumentException("Faltan canales");
        canales = Map.copyOf(mapa);
    }

    public CanalNotificacion crear(CanalPreferido preferencia) {
        return canales.get(Objects.requireNonNull(preferencia));
    }
}
