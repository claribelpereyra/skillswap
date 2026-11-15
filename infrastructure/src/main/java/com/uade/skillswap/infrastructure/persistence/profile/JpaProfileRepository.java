package com.uade.skillswap.infrastructure.persistence.profile;

import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.Profile;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JpaProfileRepository implements ProfileRepository {

    @Override
    public Profile save(Profile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        return EntityManagerContext.withEntityManager(em -> {
            ProfileJpaEntity entity;
            if (profile.getId() == null) {
                entity = ProfileMapper.newEntity(profile);
                em.persist(entity);
            } else {
                entity = em.find(ProfileJpaEntity.class, profile.getId());
                if (entity == null) {
                    throw new IllegalStateException("Profile " + profile.getId() + " does not exist");
                }
                ProfileMapper.copyMutableState(profile, entity);
                em.lock(entity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            }
            em.flush();
            return ProfileMapper.toDomain(entity);
        });
    }

    @Override
    public Optional<Profile> findByUserId(UserId userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return EntityManagerContext.withEntityManager(em -> {
            // Single fetch query for both element collections avoids N+1 reads.
            List<ProfileJpaEntity> results = em.createQuery("""
                            SELECT DISTINCT p FROM ProfileJpaEntity p
                            LEFT JOIN FETCH p.skillIds
                            LEFT JOIN FETCH p.interests
                            WHERE p.userId = :userId
                            """, ProfileJpaEntity.class)
                    .setParameter("userId", userId.value())
                    .getResultList();
            return results.stream().findFirst().map(ProfileMapper::toDomain);
        });
    }

    @Override
    public boolean existsByUserId(UserId userId) {
        if (userId == null) {
            return false;
        }
        return EntityManagerContext.withEntityManager(em -> {
            Long count = em.createQuery(
                            "SELECT COUNT(p.id) FROM ProfileJpaEntity p WHERE p.userId = :userId", Long.class)
                    .setParameter("userId", userId.value())
                    .getSingleResult();
            return count != null && count > 0;
        });
    }
}
