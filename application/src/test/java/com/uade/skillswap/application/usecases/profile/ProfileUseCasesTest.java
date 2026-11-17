package com.uade.skillswap.application.usecases.profile;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.profile.CreateProfileCommand;
import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.application.dto.profile.UpdateProfileCommand;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.profile.InactiveProfileException;
import com.uade.skillswap.domain.profile.InvalidProfileException;
import com.uade.skillswap.domain.profile.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileUseCasesTest {

    private static final UserId ANA = new UserId("ana");
    private static final ZoneId UTC = ZoneId.of("UTC");

    @Mock
    private ProfileRepository profileRepository;

    private final UnitOfWork immediateUnitOfWork = new UnitOfWork() {
        @Override
        public <T> T execute(Supplier<T> work) {
            return work.get();
        }
    };

    private CreateProfileService createService;
    private UpdateProfileService updateService;
    private ManageSkillsService manageSkillsService;
    private DeactivateProfileService deactivateService;

    @BeforeEach
    void setUp() {
        createService = new CreateProfileService(profileRepository, immediateUnitOfWork, java.time.Clock.systemUTC());
        updateService = new UpdateProfileService(profileRepository, immediateUnitOfWork);
        manageSkillsService = new ManageSkillsService(profileRepository, immediateUnitOfWork);
        deactivateService = new DeactivateProfileService(profileRepository, immediateUnitOfWork);

        lenient().when(profileRepository.save(any(Profile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsProfileWhenUserHasNoExistingProfile() {
        when(profileRepository.existsByUserId(ANA)).thenReturn(false);

        ProfileView view = createService.create(
                new CreateProfileCommand(ANA, "Ana", "Java mentor", UTC, AccountType.PROFESSIONAL));

        assertThat(view.userId()).isEqualTo("ana");
        assertThat(view.displayName()).isEqualTo("Ana");
        assertThat(view.accountType()).isEqualTo(AccountType.PROFESSIONAL);
        assertThat(view.active()).isTrue();
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void rejectsDuplicateProfileCreationOrNullCommand() {
        assertThatThrownBy(() -> createService.create(null))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The userId cannot be null");
        assertThatThrownBy(() -> createService.create(
                new CreateProfileCommand(null, "Ana", "Bio", UTC, AccountType.PROFESSIONAL)))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The userId cannot be null");

        when(profileRepository.existsByUserId(ANA)).thenReturn(true);
        assertThatThrownBy(() -> createService.create(
                new CreateProfileCommand(ANA, "Ana", "Bio", UTC, AccountType.PROFESSIONAL)))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("A profile already exists for user ana");
        verify(profileRepository, never()).save(any());
    }

    @Test
    void updatesProfileDetailsAndAvailabilityForOwnedProfile() {
        Profile existing = existingProfile(true);
        when(profileRepository.findByUserId(ANA)).thenReturn(Optional.of(existing));

        ProfileView updated = updateService.updateDetails(
                new UpdateProfileCommand(ANA, "Ana Maria", "Clean Architecture", UTC, "Weekdays 18-21"));

        assertThat(updated.displayName()).isEqualTo("Ana Maria");
        assertThat(updated.bio()).isEqualTo("Clean Architecture");
        assertThat(updated.availability()).isEqualTo("Weekdays 18-21");

        ProfileView withoutAvailabilityChange = updateService.updateDetails(
                new UpdateProfileCommand(ANA, "Ana M", "Updated bio", UTC, null));
        assertThat(withoutAvailabilityChange.availability()).isEqualTo("Weekdays 18-21");

        ProfileView updatedAvailOnly = updateService.updateAvailability(ANA, "Saturdays morning");
        assertThat(updatedAvailOnly.availability()).isEqualTo("Saturdays morning");
    }

    @Test
    void updateRejectsNullCommandMissingProfileAndInactiveProfile() {
        assertThatThrownBy(() -> updateService.updateDetails(null))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The update command cannot be null");
        assertThatThrownBy(() -> updateService.updateAvailability(null, "Weekends"))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The userId cannot be null");

        when(profileRepository.findByUserId(ANA)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> updateService.updateAvailability(ANA, "Weekends"))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Profile not found with id ana");

        when(profileRepository.findByUserId(ANA)).thenReturn(Optional.of(existingProfile(false)));
        assertThatThrownBy(() -> updateService.updateDetails(
                new UpdateProfileCommand(ANA, "Ana", "Bio", UTC, null)))
                .isExactlyInstanceOf(InactiveProfileException.class)
                .hasMessage("The operation cannot be completed because the profile is inactive");
    }

    @Test
    void managesSkillIdsAndInterestsAndRejectsInvalidInputs() {
        Profile existing = existingProfile(true);
        when(profileRepository.findByUserId(ANA)).thenReturn(Optional.of(existing));

        ProfileView afterAddSkill = manageSkillsService.addSkill(ANA, 5L);
        assertThat(afterAddSkill.skillIds()).containsExactly(5L);

        ProfileView afterRemoveSkill = manageSkillsService.removeSkill(ANA, 5L);
        assertThat(afterRemoveSkill.skillIds()).isEmpty();

        ProfileView afterAddInterest = manageSkillsService.addInterest(ANA, "DDD");
        assertThat(afterAddInterest.interests()).containsExactly("DDD");

        ProfileView afterRemoveInterest = manageSkillsService.removeInterest(ANA, "DDD");
        assertThat(afterRemoveInterest.interests()).isEmpty();

        assertThatThrownBy(() -> manageSkillsService.addSkill(ANA, 0L))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The skillId must be positive");
        assertThatThrownBy(() -> manageSkillsService.removeSkill(ANA, -3L))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The skillId must be positive");
        assertThatThrownBy(() -> manageSkillsService.addInterest(ANA, null))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The interest cannot be null or blank");
        assertThatThrownBy(() -> manageSkillsService.removeInterest(ANA, "   "))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The interest cannot be null or blank");
    }

    @Test
    void deactivatesAndReactivatesOwnedProfile() {
        Profile existing = existingProfile(true);
        when(profileRepository.findByUserId(ANA)).thenReturn(Optional.of(existing));

        ProfileView deactivated = deactivateService.deactivate(ANA);
        assertThat(deactivated.active()).isFalse();

        ProfileView reactivated = deactivateService.reactivate(ANA);
        assertThat(reactivated.active()).isTrue();

        assertThatThrownBy(() -> deactivateService.deactivate(null))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The userId cannot be null");
    }

    private static Profile existingProfile(boolean active) {
        return Profile.rebuild(1L, ANA, "Ana", "Mentor", UTC, AccountType.PROFESSIONAL,
                4, 1, Set.of(), Set.of(), null, active, Instant.parse("2026-11-01T10:00:00Z"));
    }
}
