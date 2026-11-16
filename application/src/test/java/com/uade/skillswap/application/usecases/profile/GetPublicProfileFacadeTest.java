package com.uade.skillswap.application.usecases.profile;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.profile.PublicProfileView;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.application.ports.out.profile.ActivePublicationsQuery;
import com.uade.skillswap.application.ports.out.profile.ProfileRepository;
import com.uade.skillswap.application.ports.out.rating.RatingRepository;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.profile.Availability;
import com.uade.skillswap.domain.profile.InvalidProfileException;
import com.uade.skillswap.domain.profile.Profile;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.domain.publication.PublicationType;
import com.uade.skillswap.domain.rating.Rating;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPublicProfileFacadeTest {

    private static final UserId ANA = new UserId("ana");

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private ActivePublicationsQuery activePublicationsQuery;
    @Mock
    private RatingRepository ratingRepository;

    @Test
    void aggregatesPublicProfileSummaryActivePublicationsAndRatings() {
        GetPublicProfileFacade facade = new GetPublicProfileFacade(
                profileRepository, activePublicationsQuery, ratingRepository);

        Profile profile = Profile.rebuild(1L, ANA, "Ana Perez", "Java dev", ZoneId.of("UTC"),
                AccountType.PROFESSIONAL, 9, 2, Set.of(10L, 2L), Set.of("Rust"),
                new Availability("Weeknights"), true, Instant.parse("2026-11-01T10:00:00Z"));
        PublicationView publication = new PublicationView(
                7L, "ana", PublicationType.MENTORING, PublicationStatus.PUBLISHED,
                "Java Mentoring", "Deep dive into JVM", 1L, Modality.REMOTE, 60L, 2, 2, 100L,
                Set.of("java"), List.of());
        Rating rating = Rating.rebuild(10L, 5L, new UserId("bruno"), ANA, 5, "Excelente",
                Instant.parse("2026-11-10T12:00:00Z"));

        when(profileRepository.findByUserId(ANA)).thenReturn(Optional.of(profile));
        when(activePublicationsQuery.findActiveByUserId(ANA, PageRequest.first()))
                .thenReturn(new Page<>(List.of(publication), PageRequest.first(), 3L));
        when(ratingRepository.findByRecipientId(ANA, PageRequest.first()))
                .thenReturn(new Page<>(List.of(rating), PageRequest.first(), 1));

        PublicProfileView view = facade.getPublicProfile(ANA);

        assertThat(view.userId()).isEqualTo("ana");
        assertThat(view.displayName()).isEqualTo("Ana Perez");
        assertThat(view.reputation()).isEqualTo(4.5);
        assertThat(view.ratingCount()).isEqualTo(2);
        assertThat(view.skillIds()).containsExactly(2L, 10L);
        assertThat(view.activePublications().totalItems()).isEqualTo(3L);
        assertThat(view.activePublications().items()).singleElement().satisfies(p -> {
            assertThat(p.id()).isEqualTo(7L);
            assertThat(p.title()).isEqualTo("Java Mentoring");
        });
        assertThat(view.recentRatings().items()).singleElement().satisfies(r -> {
            assertThat(r.authorId()).isEqualTo("bruno");
            assertThat(r.score()).isEqualTo(5);
            assertThat(r.comment()).isEqualTo("Excelente");
        });
    }

    @Test
    void throwsWhenTargetUserIdIsNullOrProfileDoesNotExist() {
        GetPublicProfileFacade facade = new GetPublicProfileFacade(
                profileRepository, activePublicationsQuery, ratingRepository);

        assertThatThrownBy(() -> facade.getPublicProfile(null, null, PageRequest.first()))
                .isExactlyInstanceOf(InvalidProfileException.class)
                .hasMessage("The target userId cannot be null");

        when(profileRepository.findByUserId(ANA)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> facade.getPublicProfile(ANA, (PageRequest) null))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Profile not found with id ana");
    }
}
