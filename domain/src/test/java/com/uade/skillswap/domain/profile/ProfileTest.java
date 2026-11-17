package com.uade.skillswap.domain.profile;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.rating.Rating;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileTest {

    private static final UserId USER_ID = new UserId("ana");
    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Instant CREATED_AT = Instant.parse("2026-11-01T12:00:00Z");

    @Test
    void aValidProfileStartsActiveWithZeroReputationAndUnspecifiedAvailability() {
        Profile profile = Profile.create(USER_ID, "  Ana Perez  ", "  Java mentor  ", BUENOS_AIRES,
                AccountType.PROFESSIONAL, CREATED_AT);

        assertThat(profile.getId()).isNull();
        assertThat(profile.getUserId()).isEqualTo(USER_ID);
        assertThat(profile.getDisplayName()).isEqualTo("Ana Perez");
        assertThat(profile.getBio()).isEqualTo("Java mentor");
        assertThat(profile.getTimeZone()).isEqualTo(BUENOS_AIRES);
        assertThat(profile.getAccountType()).isEqualTo(AccountType.PROFESSIONAL);
        assertThat(profile.getReputation()).isZero();
        assertThat(profile.getRatingSum()).isZero();
        assertThat(profile.getRatingCount()).isZero();
        assertThat(profile.getSkillIds()).isEmpty();
        assertThat(profile.getInterests()).isEmpty();
        assertThat(profile.getAvailability()).isEqualTo(Availability.UNSPECIFIED);
        assertThat(profile.isActive()).isTrue();
        assertThat(profile.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void creatingAProfileDefaultsNullBioTimeZoneAndTimestamp() {
        Profile profile = Profile.create(USER_ID, "Org Tech", null, null, AccountType.ORGANIZATION);

        assertThat(profile.getBio()).isEmpty();
        assertThat(profile.getTimeZone()).isEqualTo(ZoneId.systemDefault());
        assertThat(profile.getAccountType()).isEqualTo(AccountType.ORGANIZATION);
        assertThat(profile.getCreatedAt()).isNotNull();
    }

    @Test
    void creatingAProfileWithInvalidInitialDataThrowsInvalidProfileException() {
        assertThatThrownBy(() -> Profile.create(null, "Ana", "Bio", BUENOS_AIRES, AccountType.PROFESSIONAL))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The userId cannot be null");
        assertThatThrownBy(() -> Profile.create(USER_ID, "  ", "Bio", BUENOS_AIRES, AccountType.PROFESSIONAL))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The display name cannot be null or blank");
        assertThatThrownBy(() -> Profile.create(USER_ID, null, "Bio", BUENOS_AIRES, AccountType.PROFESSIONAL))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The display name cannot be null or blank");
        assertThatThrownBy(() -> Profile.create(USER_ID, "Ana", "Bio", BUENOS_AIRES, null))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The account type cannot be null");
    }

    @Test
    void rebuildingAProfileRestoresStateAndValidatesStoredInvariants() {
        Profile rebuilt = Profile.rebuild(42L, USER_ID, "Ana", "Bio", BUENOS_AIRES,
                AccountType.PROFESSIONAL, 9, 2, Set.of(101L), Set.of("Docker"),
                new Availability("Weeknights"), true, CREATED_AT);

        assertThat(rebuilt.getId()).isEqualTo(42L);
        assertThat(rebuilt.getRatingSum()).isEqualTo(9);
        assertThat(rebuilt.getRatingCount()).isEqualTo(2);
        assertThat(rebuilt.getReputation()).isEqualTo(4.5);
        assertThat(rebuilt.getSkillIds()).containsExactly(101L);
        assertThat(rebuilt.getInterests()).containsExactly("Docker");
        assertThat(rebuilt.getAvailability()).isEqualTo(new Availability("Weeknights"));

        Profile nullSets = Profile.rebuild(43L, USER_ID, "Ana", null, null,
                AccountType.ORGANIZATION, 0, 0, null, null, null, false, null);
        assertThat(nullSets.getSkillIds()).isEmpty();
        assertThat(nullSets.getInterests()).isEmpty();
        assertThat(nullSets.getAvailability()).isEqualTo(Availability.UNSPECIFIED);
        assertThat(nullSets.isActive()).isFalse();

        assertThatThrownBy(() -> Profile.rebuild(null, USER_ID, "Ana", "Bio", BUENOS_AIRES,
                AccountType.PROFESSIONAL, 0, 0, Set.of(), Set.of(), Availability.UNSPECIFIED, true, CREATED_AT))
                .isExactlyInstanceOf(NullPointerException.class)
                .hasMessage("A stored profile must have an id");
        assertThatThrownBy(() -> Profile.rebuild(1L, USER_ID, "Ana", "Bio", BUENOS_AIRES,
                AccountType.PROFESSIONAL, 0, -1, Set.of(), Set.of(), Availability.UNSPECIFIED, true, CREATED_AT))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("Rating count cannot be negative");
        assertThatThrownBy(() -> Profile.rebuild(1L, USER_ID, "Ana", "Bio", BUENOS_AIRES,
                AccountType.PROFESSIONAL, 11, 2, Set.of(), Set.of(), Availability.UNSPECIFIED, true, CREATED_AT))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("Rating sum is inconsistent with rating count");
        assertThatThrownBy(() -> Profile.rebuild(1L, USER_ID, "Ana", "Bio", BUENOS_AIRES,
                AccountType.PROFESSIONAL, 1, 2, Set.of(), Set.of(), Availability.UNSPECIFIED, true, CREATED_AT))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("Rating sum is inconsistent with rating count");

        Set<Long> nullElementSkillSet = new HashSet<>();
        nullElementSkillSet.add(null);
        assertThatThrownBy(() -> Profile.rebuild(1L, USER_ID, "Ana", "Bio", BUENOS_AIRES,
                AccountType.PROFESSIONAL, 0, 0, nullElementSkillSet, Set.of(), Availability.UNSPECIFIED, true, CREATED_AT))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The skill id must be positive");
        assertThatThrownBy(() -> Profile.rebuild(1L, USER_ID, "Ana", "Bio", BUENOS_AIRES,
                AccountType.PROFESSIONAL, 0, 0, Set.of(0L), Set.of(), Availability.UNSPECIFIED, true, CREATED_AT))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The skill id must be positive");
    }

    @Test
    void activeProfileSupportsUpdatingDetailsSkillIdsInterestsAndAvailability() {
        Profile profile = Profile.create(USER_ID, "Ana", "Old", BUENOS_AIRES, AccountType.PROFESSIONAL);
        ZoneId madrid = ZoneId.of("Europe/Madrid");

        profile.updateDetails("  Ana M  ", "  New bio  ", madrid);
        assertThat(profile.getDisplayName()).isEqualTo("Ana M");
        assertThat(profile.getBio()).isEqualTo("New bio");
        assertThat(profile.getTimeZone()).isEqualTo(madrid);

        profile.updateDetails("Ana M", null, null);
        assertThat(profile.getBio()).isEmpty();
        assertThat(profile.getTimeZone()).isEqualTo(madrid);

        assertThatThrownBy(() -> profile.updateDetails("   ", "Bio", madrid))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The new display name cannot be null or blank");
        assertThatThrownBy(() -> profile.updateDetails(null, "Bio", madrid))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The new display name cannot be null or blank");

        profile.addSkill(101L);
        profile.addSkill(101L);
        profile.addSkill(102L);
        assertThat(profile.getSkillIds()).containsExactlyInAnyOrder(101L, 102L);
        assertThatThrownBy(() -> profile.addSkill(0L))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The skill id must be positive");

        profile.removeSkill(101L);
        assertThat(profile.getSkillIds()).containsExactly(102L);

        profile.addInterest("  Clean Architecture  ");
        profile.addInterest("   ");
        profile.addInterest(null);
        assertThat(profile.getInterests()).containsExactly("Clean Architecture");

        profile.removeInterest("Clean Architecture");
        profile.removeInterest(null);
        assertThat(profile.getInterests()).isEmpty();

        profile.updateAvailability(new Availability("  Mondays 18-20  "));
        assertThat(profile.getAvailability().description()).isEqualTo("Mondays 18-20");

        profile.updateAvailability(null);
        assertThat(profile.getAvailability()).isEqualTo(Availability.UNSPECIFIED);
    }

    @Test
    void inactiveProfileRejectsMutationsAndCanBeReactivated() {
        Profile profile = Profile.create(USER_ID, "Ana", "Bio", BUENOS_AIRES, AccountType.PROFESSIONAL);
        profile.deactivate();

        assertThat(profile.isActive()).isFalse();
        assertThatThrownBy(profile::requireActive)
                .isExactlyInstanceOf(InactiveProfileException.class)
                .hasMessage("The operation cannot be completed because the profile is inactive");
        assertThatThrownBy(() -> profile.updateDetails("New", "Bio", BUENOS_AIRES))
                .isExactlyInstanceOf(InactiveProfileException.class)
                .hasMessage("The operation cannot be completed because the profile is inactive");
        assertThatThrownBy(() -> profile.addSkill(101L))
                .isExactlyInstanceOf(InactiveProfileException.class)
                .hasMessage("The operation cannot be completed because the profile is inactive");
        assertThatThrownBy(() -> profile.removeSkill(101L))
                .isExactlyInstanceOf(InactiveProfileException.class)
                .hasMessage("The operation cannot be completed because the profile is inactive");
        assertThatThrownBy(() -> profile.addInterest("Testing"))
                .isExactlyInstanceOf(InactiveProfileException.class)
                .hasMessage("The operation cannot be completed because the profile is inactive");
        assertThatThrownBy(() -> profile.removeInterest("Testing"))
                .isExactlyInstanceOf(InactiveProfileException.class)
                .hasMessage("The operation cannot be completed because the profile is inactive");
        assertThatThrownBy(() -> profile.updateAvailability(new Availability("Weekends")))
                .isExactlyInstanceOf(InactiveProfileException.class)
                .hasMessage("The operation cannot be completed because the profile is inactive");

        profile.reactivate();
        assertThat(profile.isActive()).isTrue();
        profile.requireActive();
    }

    private static Rating ratingFor(UserId recipient, int score) {
        return Rating.create(1L, new UserId("author"), recipient, score, null);
    }

    @Test
    void registeringRatingsCalculatesExactAverageWithoutCumulativeRoundingDrift() {
        Profile profile = Profile.create(USER_ID, "Ana", "Bio", BUENOS_AIRES, AccountType.PROFESSIONAL);

        for (int score : List.of(1, 1, 1, 1, 1, 2, 1)) {
            profile.registerRating(ratingFor(USER_ID, score));
        }

        assertThat(profile.getRatingSum()).isEqualTo(8);
        assertThat(profile.getRatingCount()).isEqualTo(7);
        assertThat(profile.getReputation()).isEqualTo(1.14);

        assertThatThrownBy(() -> profile.registerRating(ratingFor(new UserId("someone-else"), 5)))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The rating is not addressed to this profile");
        assertThat(profile.getRatingCount()).isEqualTo(7);
    }

    @Test
    void toPublicSummaryExposesExpectedPublicFieldsOnly() {
        Profile profile = Profile.create(USER_ID, "Ana", "Backend dev", BUENOS_AIRES, AccountType.PROFESSIONAL);
        profile.addSkill(101L);
        profile.updateAvailability(new Availability("Evenings"));
        profile.registerRating(ratingFor(USER_ID, 5));

        PublicProfileSummary summary = profile.toPublicSummary();

        assertThat(summary.userId()).isEqualTo(USER_ID);
        assertThat(summary.displayName()).isEqualTo("Ana");
        assertThat(summary.bio()).isEqualTo("Backend dev");
        assertThat(summary.accountType()).isEqualTo(AccountType.PROFESSIONAL);
        assertThat(summary.reputation()).isEqualTo(5.0);
        assertThat(summary.ratingCount()).isEqualTo(1);
        assertThat(summary.skillIds()).containsExactly(101L);
        assertThat(summary.availability()).isEqualTo(new Availability("Evenings"));
        assertThat(summary.active()).isTrue();
    }

    @Test
    void availabilityValueObjectNormalizesBlankAndNullInputs() {
        Availability blankAvailability = new Availability("   ");
        Availability nullAvailability = new Availability(null);
        Availability validAvailability = new Availability("  Sat morning ");

        assertThat(blankAvailability).isEqualTo(Availability.UNSPECIFIED);
        assertThat(nullAvailability).isEqualTo(Availability.UNSPECIFIED);
        assertThat(validAvailability.description()).isEqualTo("Sat morning");
    }

    @Test
    void profileEqualityIsBasedOnUserId() {
        Profile p1 = Profile.create(USER_ID, "Ana", "Bio", BUENOS_AIRES, AccountType.PROFESSIONAL);
        Profile p2 = Profile.create(USER_ID, "Ana Other", "Other", BUENOS_AIRES, AccountType.ORGANIZATION);
        Profile other = Profile.create(new UserId("bruno"), "Bruno", "Bio", BUENOS_AIRES, AccountType.PROFESSIONAL);

        assertThat(p1).isEqualTo(p1);
        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        assertThat(p1).isNotEqualTo(other);
        assertThat(p1.equals(null)).isFalse();
        assertThat(p1.equals("not a profile")).isFalse();
    }
}
