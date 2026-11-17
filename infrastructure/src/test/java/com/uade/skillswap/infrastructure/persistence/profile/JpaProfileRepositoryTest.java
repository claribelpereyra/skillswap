package com.uade.skillswap.infrastructure.persistence.profile;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.rating.Rating;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.profile.Availability;
import com.uade.skillswap.domain.profile.Profile;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaProfileRepositoryTest extends PersistenceTestBase {

    private static final UserId ANA = new UserId("ana");
    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    private final JpaProfileRepository repository = new JpaProfileRepository();

    @Test
    void savesLoadsAndUpdatesProfileWithSkillIdsInterestsReputationAndDeactivation() {
        Profile profile = Profile.create(ANA, "Ana Perez", "Backend mentor", BUENOS_AIRES,
                AccountType.PROFESSIONAL, Instant.parse("2026-11-01T10:00:00Z"));
        profile.addSkill(10L);
        profile.addSkill(20L);
        profile.addInterest("Architecture");
        profile.updateAvailability(new Availability("Weekdays 19-21"));
        for (int score : new int[]{1, 1, 1, 1, 1, 2, 1}) {
            profile.registerRating(Rating.create(1L, new UserId("author"), ANA, score, null));
        }

        Profile saved = unitOfWork.execute(() -> repository.save(profile));
        assertThat(saved.getId()).isNotNull();
        assertThat(repository.existsByUserId(ANA)).isTrue();
        assertThat(repository.existsByUserId(new UserId("missing"))).isFalse();
        assertThat(repository.existsByUserId(null)).isFalse();
        assertThat(repository.findByUserId(null)).isEmpty();

        Optional<Profile> reloaded = repository.findByUserId(ANA);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getSkillIds()).containsExactlyInAnyOrder(10L, 20L);
        assertThat(reloaded.get().getInterests()).containsExactly("Architecture");
        assertThat(reloaded.get().getAvailability()).isEqualTo(new Availability("Weekdays 19-21"));
        assertThat(reloaded.get().getRatingSum()).isEqualTo(8);
        assertThat(reloaded.get().getRatingCount()).isEqualTo(7);
        assertThat(reloaded.get().getReputation()).isEqualTo(1.14);

        Profile toUpdate = reloaded.get();
        toUpdate.removeSkill(20L);
        toUpdate.addSkill(30L);
        toUpdate.updateDetails("Ana P", "Updated bio", ZoneId.of("UTC"));
        toUpdate.deactivate();
        unitOfWork.execute(() -> repository.save(toUpdate));

        Profile afterUpdate = repository.findByUserId(ANA).orElseThrow();
        assertThat(afterUpdate.getDisplayName()).isEqualTo("Ana P");
        assertThat(afterUpdate.getBio()).isEqualTo("Updated bio");
        assertThat(afterUpdate.getTimeZone()).isEqualTo(ZoneId.of("UTC"));
        assertThat(afterUpdate.getSkillIds()).containsExactlyInAnyOrder(10L, 30L);
        assertThat(afterUpdate.isActive()).isFalse();
    }

    @Test
    void databaseEnforcesUniqueUserIdPerProfile() {
        unitOfWork.execute(() -> repository.save(
                Profile.create(ANA, "Ana", "Bio", BUENOS_AIRES, AccountType.PROFESSIONAL)));

        Profile duplicate = Profile.create(ANA, "Ana 2", "Bio 2", BUENOS_AIRES, AccountType.ORGANIZATION);
        assertThatThrownBy(() -> unitOfWork.execute(() -> repository.save(duplicate)))
                .isInstanceOf(PersistenceException.class);
        assertThat(count("profile")).isEqualTo(1L);
    }
}
