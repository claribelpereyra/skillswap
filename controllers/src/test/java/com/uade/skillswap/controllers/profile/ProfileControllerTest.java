package com.uade.skillswap.controllers.profile;

import com.uade.skillswap.application.common.ConcurrentUpdateException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.profile.CreateProfileCommand;
import com.uade.skillswap.application.dto.profile.ProfileView;
import com.uade.skillswap.application.dto.profile.PublicProfileView;
import com.uade.skillswap.application.dto.profile.UpdateProfileCommand;
import com.uade.skillswap.application.ports.in.profile.CreateProfileUseCase;
import com.uade.skillswap.application.ports.in.profile.DeactivateProfileUseCase;
import com.uade.skillswap.application.ports.in.profile.GetPublicProfileUseCase;
import com.uade.skillswap.application.ports.in.profile.ManageSkillsUseCase;
import com.uade.skillswap.application.ports.in.profile.UpdateProfileUseCase;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.ControllerResult.ErrorType;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.profile.InactiveProfileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private static final UserId ANA = new UserId("ana");
    private static final UserId BRUNO = new UserId("bruno");

    @Mock
    private CreateProfileUseCase createProfile;
    @Mock
    private UpdateProfileUseCase updateProfile;
    @Mock
    private ManageSkillsUseCase manageSkills;
    @Mock
    private DeactivateProfileUseCase deactivateProfile;
    @Mock
    private GetPublicProfileUseCase getPublicProfile;
    @Mock
    private IdentityService identityService;

    private ProfileController controller;

    @BeforeEach
    void setUp() {
        lenient().when(identityService.currentUserId()).thenReturn(ANA);
        controller = new ProfileControllerImpl(
                createProfile, updateProfile, manageSkills, deactivateProfile, getPublicProfile, identityService);
    }

    @Test
    void delegatesCreateAndUpdateFormsConvertingEnumsAndTimeZones() {
        ProfileView expected = sampleProfileView(true);
        CreateProfileCommand expectedCreate = new CreateProfileCommand(
                ANA, "Ana", "Mentor", ZoneId.of("America/Argentina/Buenos_Aires"), AccountType.PROFESSIONAL);
        when(createProfile.create(expectedCreate)).thenReturn(expected);

        ControllerResult<ProfileView> created = controller.createProfile("ana",
                new CreateProfileForm("Ana", "Mentor", "America/Argentina/Buenos_Aires", "professional"));
        assertThat(created.isSuccess()).isTrue();
        assertThat(created.getData()).isEqualTo(expected);
        assertThat(created.getErrorType()).isNull();

        UpdateProfileCommand expectedUpdate = new UpdateProfileCommand(ANA, "Ana M", "Updated", null, "Evenings");
        when(updateProfile.updateDetails(expectedUpdate)).thenReturn(expected);
        ControllerResult<ProfileView> updated = controller.updateProfile("ana",
                new UpdateProfileForm("Ana M", "Updated", "   ", "Evenings"));
        assertThat(updated.isSuccess()).isTrue();
        assertThat(updated.getData()).isEqualTo(expected);

        when(updateProfile.updateAvailability(ANA, "Mornings")).thenReturn(expected);
        assertThat(controller.updateAvailability("ana", "Mornings").isSuccess()).isTrue();
    }

    @Test
    void delegatesSkillInterestLifecycleAndPublicProfileQueries() {
        ProfileView activeView = sampleProfileView(true);
        ProfileView inactiveView = sampleProfileView(false);
        PublicProfileView publicView = new PublicProfileView("ana", "Ana", "Mentor", AccountType.PROFESSIONAL,
                5.0, 1, Set.of(1L), "Evenings", true, Page.empty(PageRequest.first()), Page.empty(new PageRequest(0, 10)));

        when(manageSkills.addSkill(ANA, 1L)).thenReturn(activeView);
        when(manageSkills.removeSkill(ANA, 1L)).thenReturn(activeView);
        when(manageSkills.addInterest(ANA, "Rust")).thenReturn(activeView);
        when(manageSkills.removeInterest(ANA, "Rust")).thenReturn(activeView);
        when(deactivateProfile.deactivate(ANA)).thenReturn(inactiveView);
        when(deactivateProfile.reactivate(ANA)).thenReturn(activeView);
        when(getPublicProfile.getPublicProfile(ANA, new PageRequest(0, 10))).thenReturn(publicView);

        assertThat(controller.addSkill("ana", 1L).isSuccess()).isTrue();
        assertThat(controller.removeSkill("ana", 1L).isSuccess()).isTrue();
        assertThat(controller.addInterest("ana", "Rust").isSuccess()).isTrue();
        assertThat(controller.removeInterest("ana", "Rust").isSuccess()).isTrue();
        assertThat(controller.deactivate("ana").getData().active()).isFalse();
        assertThat(controller.reactivate("ana").getData().active()).isTrue();
        assertThat(controller.getPublicProfile("ana", 0, 10).getData()).isEqualTo(publicView);

        verify(getPublicProfile).getPublicProfile(ANA, new PageRequest(0, 10));
    }

    @Test
    void returnsInvalidInputWhenCreateFormIsNull() {
        ControllerResult<ProfileView> result = controller.createProfile("ana", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(result.getMessage()).isEqualTo("The profile form is required");
        verifyNoInteractions(createProfile);
    }

    @Test
    void returnsInvalidInputWhenUpdateFormIsNull() {
        ControllerResult<ProfileView> result = controller.updateProfile("ana", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(result.getMessage()).isEqualTo("The profile update form is required");
        verifyNoInteractions(updateProfile);
    }

    @Test
    void returnsInvalidInputWhenAccountTypeIsMissingOrUnknown() {
        ControllerResult<ProfileView> missing = controller.createProfile("ana",
                new CreateProfileForm("Ana", "Bio", "UTC", " "));
        ControllerResult<ProfileView> unknown = controller.createProfile("ana",
                new CreateProfileForm("Ana", "Bio", "UTC", "INVALID_TYPE"));

        assertThat(missing.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(missing.getMessage()).isEqualTo("The accountType is required");
        assertThat(unknown.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(unknown.getMessage()).isEqualTo("Unknown accountType: INVALID_TYPE");
        verifyNoInteractions(createProfile);
    }

    @Test
    void returnsInvalidInputWhenTimeZoneIsInvalid() {
        ControllerResult<ProfileView> result = controller.createProfile("ana",
                new CreateProfileForm("Ana", "Bio", "Not/A_Zone", "PROFESSIONAL"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(result.getMessage()).isEqualTo("Invalid timeZone: Not/A_Zone");
        verifyNoInteractions(createProfile);
    }

    @Test
    void returnsInvalidInputWhenPublicProfileTargetOrPageIsInvalid() {
        ControllerResult<PublicProfileView> blankTarget = controller.getPublicProfile("", 0, 10);
        ControllerResult<PublicProfileView> negativePage = controller.getPublicProfile("ana", -1, 10);

        assertThat(blankTarget.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(blankTarget.getMessage()).isEqualTo("The targetUserId is required");
        assertThat(negativePage.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        assertThat(negativePage.getMessage()).isEqualTo("Page number cannot be negative: -1");
        verifyNoInteractions(getPublicProfile);
    }

    @Test
    void returnsAccessDeniedWhenActiveUserIsMissing() {
        ControllerResult<ProfileView> result = controller.createProfile(" ",
                new CreateProfileForm("Ana", "Bio", "UTC", "PROFESSIONAL"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(result.getMessage()).isEqualTo("No active user");
        verifyNoInteractions(createProfile);
    }

    @Test
    void returnsAccessDeniedWhenActorDiffersFromAuthenticatedUser() {
        when(identityService.currentUserId()).thenReturn(BRUNO);

        ControllerResult<ProfileView> result = controller.deactivate("ana");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(result.getMessage()).isEqualTo("User ana is not allowed to deactivate profile");
        verifyNoInteractions(deactivateProfile);
    }

    @Test
    void returnsAccessDeniedWhenNoUserIsSignedIn() {
        when(identityService.currentUserId()).thenReturn(null);

        ControllerResult<ProfileView> result = controller.addSkill("ana", 1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(result.getMessage()).isEqualTo("User ana is not allowed to add skill");
        verifyNoInteractions(manageSkills);
    }

    @Test
    void returnsNotFoundWhenPublicProfileDoesNotExist() {
        when(getPublicProfile.getPublicProfile(ANA, new PageRequest(0, 10)))
                .thenThrow(new ResourceNotFoundException("Profile", "ana"));

        ControllerResult<PublicProfileView> result = controller.getPublicProfile("ana", 0, 10);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.getMessage()).isEqualTo("Profile not found with id ana");
    }

    @Test
    void returnsBusinessRuleWhenOperatingOnInactiveProfile() {
        when(manageSkills.addSkill(ANA, 1L)).thenThrow(new InactiveProfileException("Inactive profile"));

        ControllerResult<ProfileView> result = controller.addSkill("ana", 1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorType()).isEqualTo(ErrorType.BUSINESS_RULE);
        assertThat(result.getMessage()).isEqualTo("Inactive profile");
    }

    @Test
    void returnsConflictOnConcurrentUpdateAndUnexpectedOnUnhandledError() {
        ConcurrentUpdateException conflict = new ConcurrentUpdateException(new RuntimeException("version mismatch"));
        when(deactivateProfile.deactivate(ANA)).thenThrow(conflict);
        when(manageSkills.removeSkill(ANA, 1L)).thenThrow(new IllegalStateException("db down"));

        ControllerResult<ProfileView> conflictResult = controller.deactivate("ana");
        ControllerResult<ProfileView> unexpectedResult = controller.removeSkill("ana", 1L);

        assertThat(conflictResult.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        assertThat(conflictResult.getMessage()).isEqualTo(conflict.getMessage());
        assertThat(unexpectedResult.getErrorType()).isEqualTo(ErrorType.UNEXPECTED);
        assertThat(unexpectedResult.getMessage()).isEqualTo("An unexpected error occurred");
    }

    private static ProfileView sampleProfileView(boolean active) {
        return new ProfileView(1L, "ana", "Ana", "Mentor", "UTC", AccountType.PROFESSIONAL,
                5.0, 1, Set.of(1L), Set.of("Rust"), "Evenings", active, Instant.parse("2026-11-01T10:00:00Z"));
    }
}
