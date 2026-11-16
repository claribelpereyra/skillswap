package com.uade.skillswap.infrastructure.persistence.profile;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.profile.Profile;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuthorProfileQueryTest extends PersistenceTestBase {

    private final JpaProfileRepository profileRepository = new JpaProfileRepository();
    private final JpaAuthorProfileQuery query = new JpaAuthorProfileQuery();

    @Test
    void returnsAccountTypeOnlyWhenUserHasAnActiveProfile() {
        UserId org = new UserId("techlab");
        UserId pro = new UserId("ana");

        Profile orgProfile = Profile.create(org, "TechLab", "Workshops", ZoneId.of("UTC"), AccountType.ORGANIZATION);
        Profile proProfile = Profile.create(pro, "Ana", "Mentor", ZoneId.of("UTC"), AccountType.PROFESSIONAL);

        unitOfWork.execute(() -> {
            profileRepository.save(orgProfile);
            profileRepository.save(proProfile);
        });

        assertThat(query.accountTypeOf(org)).contains(AccountType.ORGANIZATION);
        assertThat(query.accountTypeOf(pro)).contains(AccountType.PROFESSIONAL);
        assertThat(query.accountTypeOf(new UserId("unknown"))).isEmpty();
        assertThat(query.accountTypeOf(null)).isEmpty();

        unitOfWork.execute(() -> {
            Profile stored = profileRepository.findByUserId(pro).orElseThrow();
            stored.deactivate();
            profileRepository.save(stored);
        });

        assertThat(query.accountTypeOf(pro)).isEmpty();
    }
}
