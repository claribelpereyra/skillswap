package com.uade.skillswap.infrastructure.persistence.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.ports.out.publication.PublicationRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

// Implementación JPA del puerto. Guardar tiene que correr dentro de una unidad de trabajo (JpaUnitOfWork).
public class JpaPublicationRepository implements PublicationRepository {

    @Override
    public Optional<Publication> findById(long id) {
        return EntityManagerContext.withEntityManager(em ->
                Optional.ofNullable(em.find(PublicationJpaEntity.class, id)).map(PublicationMapper::toDomain));
    }

    @Override
    public Publication save(Publication publication) {
        return EntityManagerContext.withEntityManager(em -> {
            PublicationJpaEntity entity;
            if (publication.getId() == null) {
                entity = PublicationMapper.newEntity(publication);
                em.persist(entity);
            } else {
                entity = em.find(PublicationJpaEntity.class, publication.getId());
                if (entity == null) {
                    throw new IllegalStateException("Publication " + publication.getId() + " does not exist");
                }
                PublicationMapper.copyInto(publication, entity);
                // Fuerza el incremento de versión aunque solo cambie una franja, para que no se sobrevenda el cupo.
                em.lock(entity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            }
            em.flush();
            return PublicationMapper.toDomain(entity);
        });
    }

    @Override
    public Page<Publication> findByAuthor(UserId authorId, PublicationStatus status, PageRequest page) {
        return EntityManagerContext.withEntityManager(em -> {
            String filter = " WHERE p.authorId = :author" + (status == null ? "" : " AND p.status = :status");
            TypedQuery<Long> count = em.createQuery("SELECT COUNT(p) FROM PublicationJpaEntity p" + filter, Long.class)
                    .setParameter("author", authorId.value());
            TypedQuery<PublicationJpaEntity> select = em.createQuery(
                            "SELECT p FROM PublicationJpaEntity p" + filter + " ORDER BY p.id DESC",
                            PublicationJpaEntity.class)
                    .setParameter("author", authorId.value())
                    .setFirstResult(Math.toIntExact(page.offset()))
                    .setMaxResults(page.size());
            if (status != null) {
                count.setParameter("status", status);
                select.setParameter("status", status);
            }
            // Las colecciones de las publicaciones de la página se cargan en lote (@BatchSize), sin N+1.
            List<Publication> items = select.getResultList().stream().map(PublicationMapper::toDomain).toList();
            return new Page<>(items, page, count.getSingleResult());
        });
    }
}
