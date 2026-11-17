package com.uade.skillswap.infrastructure.persistence.booking;

import com.uade.skillswap.application.ports.out.BookingLabelsQuery;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import java.util.*;

public final class JpaBookingLabelsQuery implements BookingLabelsQuery {
    @Override public Map<Long,Labels> forBookings(Set<Long> ids) {
        if(ids.isEmpty())return Map.of();
        return EntityManagerContext.withEntityManager(em->{
            @SuppressWarnings("unchecked")
            List<Object[]> rows=em.createNativeQuery("""
                    SELECT b.id,p.title,COALESCE(c.display_name,b.client_id),COALESCE(s.display_name,b.provider_id)
                    FROM booking b JOIN publication p ON p.id=b.publication_id
                    LEFT JOIN profile c ON c.user_id=b.client_id
                    LEFT JOIN profile s ON s.user_id=b.provider_id
                    WHERE b.id IN (:ids)
                    """).setParameter("ids",ids).getResultList();
            Map<Long,Labels> result=new HashMap<>();
            for(var row:rows)result.put(((Number)row[0]).longValue(),new Labels((String)row[1],(String)row[2],(String)row[3]));
            return Map.copyOf(result);
        });
    }
}
