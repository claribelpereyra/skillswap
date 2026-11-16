package com.uade.skillswap.infrastructure.persistence.profile;

import com.uade.skillswap.application.ports.out.publication.AuthorProfileQuery;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;

import java.util.List;
import java.util.Optional;

public class JpaAuthorProfileQuery implements AuthorProfileQuery {

    @Override
    public Optional<AccountType> accountTypeOf(UserId userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return EntityManagerContext.withEntityManager(em -> {
            List<AccountType> results = em.createQuery(
                            "SELECT p.accountType FROM ProfileJpaEntity p WHERE p.userId = :userId AND p.active = true",
                            AccountType.class)
                    .setParameter("userId", userId.value())
                    .getResultList();
            return results.stream().findFirst();
        });
    }
}
