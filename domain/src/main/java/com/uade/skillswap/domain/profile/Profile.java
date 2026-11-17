package com.uade.skillswap.domain.profile;

import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.rating.Rating;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Profile {

    private Long id;
    private final UserId userId;
    private String displayName;
    private String bio;
    private ZoneId timeZone;
    private final AccountType accountType;
    private int ratingSum;
    private int ratingCount;
    private final Set<Long> skillIds;
    private final Set<String> interests;
    private Availability availability;
    private boolean active;
    private final Instant createdAt;

    private Profile(Long id,
                    UserId userId,
                    String displayName,
                    String bio,
                    ZoneId timeZone,
                    AccountType accountType,
                    int ratingSum,
                    int ratingCount,
                    Set<Long> skillIds,
                    Set<String> interests,
                    Availability availability,
                    boolean active,
                    Instant createdAt) {
        validateInitialData(userId, displayName, accountType);
        this.id = id;
        this.userId = userId;
        this.displayName = displayName.trim();
        this.bio = bio != null ? bio.trim() : "";
        this.timeZone = timeZone != null ? timeZone : ZoneId.systemDefault();
        this.accountType = accountType;
        this.ratingSum = ratingSum;
        this.ratingCount = ratingCount;
        this.skillIds = new HashSet<>(skillIds);
        this.interests = new HashSet<>(interests);
        this.availability = availability != null ? availability : Availability.UNSPECIFIED;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "The creation date is required");
    }

    public static Profile create(UserId userId,
                                 String displayName,
                                 String bio,
                                 ZoneId timeZone,
                                 AccountType accountType,
                                 Instant createdAt) {
        return new Profile(null, userId, displayName, bio, timeZone, accountType,
                0, 0, Set.of(), Set.of(), Availability.UNSPECIFIED, true, createdAt);
    }


    public static Profile rebuild(Long id,
                                  UserId userId,
                                  String displayName,
                                  String bio,
                                  ZoneId timeZone,
                                  AccountType accountType,
                                  int ratingSum,
                                  int ratingCount,
                                  Set<Long> skillIds,
                                  Set<String> interests,
                                  Availability availability,
                                  boolean active,
                                  Instant createdAt) {
        Objects.requireNonNull(id, "A stored profile must have an id");
        if (ratingCount < 0) {
            throw new InvalidProfileException("Rating count cannot be negative");
        }
        if (ratingSum < ratingCount || ratingSum > ratingCount * 5) {
            throw new InvalidProfileException("Rating sum is inconsistent with rating count");
        }
        Set<Long> normalizedSkills = skillIds != null ? skillIds : Set.of();
        for (Long skillId : normalizedSkills) {
            if (skillId == null || skillId <= 0) {
                throw new InvalidProfileException("The skill id must be positive");
            }
        }
        return new Profile(id, userId, displayName, bio, timeZone, accountType,
                ratingSum, ratingCount,
                normalizedSkills,
                interests != null ? interests : Set.of(),
                availability, active, createdAt);
    }

    private static void validateInitialData(UserId userId, String displayName, AccountType accountType) {
        if (userId == null) {
            throw new InvalidProfileException("The userId cannot be null");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new InvalidProfileException("The display name cannot be null or blank");
        }
        if (accountType == null) {
            throw new InvalidProfileException("The account type cannot be null");
        }
    }

    public void updateDetails(String newDisplayName, String newBio, ZoneId newTimeZone) {
        requireActive();
        if (newDisplayName == null || newDisplayName.isBlank()) {
            throw new InvalidProfileException("The new display name cannot be null or blank");
        }
        this.displayName = newDisplayName.trim();
        this.bio = newBio != null ? newBio.trim() : "";
        if (newTimeZone != null) {
            this.timeZone = newTimeZone;
        }
    }

    public void addSkill(long skillId) {
        requireActive();
        if (skillId <= 0) {
            throw new InvalidProfileException("The skill id must be positive");
        }
        this.skillIds.add(skillId);
    }

    public void removeSkill(long skillId) {
        requireActive();
        this.skillIds.remove(skillId);
    }

    public void addInterest(String interest) {
        requireActive();
        if (interest != null && !interest.isBlank()) {
            this.interests.add(interest.trim());
        }
    }

    public void removeInterest(String interest) {
        requireActive();
        if (interest != null) {
            this.interests.remove(interest.trim());
        }
    }

    public void updateAvailability(Availability newAvailability) {
        requireActive();
        this.availability = newAvailability != null ? newAvailability : Availability.UNSPECIFIED;
    }

    public void deactivate() {
        this.active = false;
    }

    public void reactivate() {
        this.active = true;
    }

    public boolean isActive() {
        return active;
    }

    public void requireActive() {
        if (!active) {
            throw new InactiveProfileException("The operation cannot be completed because the profile is inactive");
        }
    }

    public void registerRating(Rating rating) {
        Objects.requireNonNull(rating, "The rating is required");
        if (!userId.equals(rating.getRecipientId())) {
            throw new InvalidProfileException("The rating is not addressed to this profile");
        }
        addScore(rating.getScore());
    }

    private void addScore(int score) {
        if (score < 1 || score > 5) {
            throw new InvalidProfileException("The rating score must be between 1 and 5");
        }
        // Keep raw sum so incremental updates never accumulate rounding drift before display.
        this.ratingSum += score;
        this.ratingCount++;
    }

    public PublicProfileSummary toPublicSummary() {
        return new PublicProfileSummary(
                userId,
                displayName,
                bio,
                accountType,
                getReputation(),
                ratingCount,
                Collections.unmodifiableSet(skillIds),
                availability,
                active
        );
    }

    public Long getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBio() {
        return bio;
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public double getReputation() {
        return ratingCount == 0 ? 0.0 : Math.round(ratingSum * 100.0 / ratingCount) / 100.0;
    }

    public int getRatingSum() {
        return ratingSum;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public Set<Long> getSkillIds() {
        return Collections.unmodifiableSet(skillIds);
    }

    public Set<String> getInterests() {
        return Collections.unmodifiableSet(interests);
    }

    public Availability getAvailability() {
        return availability;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Profile profile = (Profile) o;
        return Objects.equals(userId, profile.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
