package com.uade.skillswap.infrastructure.persistence.booking;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.out.BookingRepository;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import jakarta.persistence.EntityManager;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class JpaBookingRepository implements BookingRepository {

    // Tiene que correr dentro de una unidad de trabajo: el flush necesita una transacción activa.
    @Override
    public Booking save(Booking booking) {
        return EntityManagerContext.withEntityManager(em -> {
            BookingJpaEntity entity = booking.getId() == null
                    ? BookingJpaEntity.create()
                    : Optional.ofNullable(em.find(BookingJpaEntity.class, booking.getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", booking.getId()));
            BookingMapper.copyToEntity(booking, entity);
            if (entity.getId() == null) {
                em.persist(entity);
            }
            em.flush();
            return BookingMapper.toDomain(entity);
        });
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return EntityManagerContext.withEntityManager(em ->
                Optional.ofNullable(em.find(BookingJpaEntity.class, id)).map(BookingMapper::toDomain));
    }

    @Override
    public Page<Booking> findByClient(UserId clientId, PageRequest page) {
        return findPage("clientId", clientId, page);
    }

    @Override
    public Page<Booking> findByProvider(UserId providerId, PageRequest page) {
        return findPage("providerId", providerId, page);
    }

    // Evita N+1: primero trae los ids de la página y después las reservas con su historial en una sola consulta
    // (fetch join). No se pagina el fetch join directo porque Hibernate paginaría en memoria.
    private Page<Booking> findPage(String userField, UserId user, PageRequest page) {
        return EntityManagerContext.withEntityManager(em -> {
            long total = em.createQuery(
                            "select count(b) from BookingJpaEntity b where b." + userField + " = :user", Long.class)
                    .setParameter("user", user.value())
                    .getSingleResult();
            List<Long> ids = em.createQuery(
                            "select b.id from BookingJpaEntity b where b." + userField + " = :user order by b.id desc",
                            Long.class)
                    .setParameter("user", user.value())
                    .setFirstResult((int) page.offset())
                    .setMaxResults(page.size())
                    .getResultList();
            return new Page<>(loadWithHistory(em, ids), page, total);
        });
    }

    private List<Booking> loadWithHistory(EntityManager em, List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return em.createQuery(
                        "select distinct b from BookingJpaEntity b left join fetch b.transitions where b.id in :ids",
                        BookingJpaEntity.class)
                .setParameter("ids", ids)
                .getResultList()
                .stream()
                .sorted(Comparator.comparing(BookingJpaEntity::getId).reversed())
                .map(BookingMapper::toDomain)
                .toList();
    }
}
