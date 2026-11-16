package com.uade.skillswap.infrastructure.persistence.profile;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.Availability;
import com.uade.skillswap.domain.profile.Profile;

import java.time.ZoneId;
import java.util.Set;

final class ProfileMapper {

    private ProfileMapper() {
    }

    static ProfileJpaEntity newEntity(Profile profile) {
        ProfileJpaEntity entity = new ProfileJpaEntity();
        entity.setUserId(profile.getUserId().value());
        entity.setAccountType(profile.getAccountType());
        entity.setCreatedAt(profile.getCreatedAt());
        copyMutableState(profile, entity);
        return entity;
    }

    static void copyMutableState(Profile profile, ProfileJpaEntity entity) {
        entity.setDisplayName(profile.getDisplayName());
        entity.setBio(profile.getBio());
        entity.setTimeZone(profile.getTimeZone().getId());
        entity.setReputation(profile.getReputation());
        entity.setRatingSum(profile.getRatingSum());
        entity.setRatingCount(profile.getRatingCount());
        entity.setAvailability(profile.getAvailability().description());
        entity.setActive(profile.isActive());

        entity.getSkillIds().clear();
        entity.getSkillIds().addAll(profile.getSkillIds());

        entity.getInterests().clear();
        entity.getInterests().addAll(profile.getInterests());
    }

    static Profile toDomain(ProfileJpaEntity entity) {
        return Profile.rebuild(
                entity.getId(),
                new UserId(entity.getUserId()),
                entity.getDisplayName(),
                entity.getBio(),
                ZoneId.of(entity.getTimeZone()),
                entity.getAccountType(),
                entity.getRatingSum(),
                entity.getRatingCount(),
                Set.copyOf(entity.getSkillIds()),
                Set.copyOf(entity.getInterests()),
                new Availability(entity.getAvailability()),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
