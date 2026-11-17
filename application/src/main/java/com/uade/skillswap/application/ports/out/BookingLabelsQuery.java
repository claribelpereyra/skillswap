package com.uade.skillswap.application.ports.out;

import java.util.Map;
import java.util.Set;

/** Proyección agrupada para presentar una página sin consultar perfiles/publicaciones por fila. */
public interface BookingLabelsQuery {
    record Labels(String title, String clientName, String providerName) { }
    Map<Long, Labels> forBookings(Set<Long> bookingIds);
}
