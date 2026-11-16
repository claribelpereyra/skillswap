package com.uade.skillswap.app.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.publication.AuthorProfileQuery;
import com.uade.skillswap.application.usecases.publication.ChangePublicationStatusService;
import com.uade.skillswap.application.usecases.publication.CreateDraftService;
import com.uade.skillswap.application.usecases.publication.EditDraftService;
import com.uade.skillswap.application.usecases.publication.GetPublicationService;
import com.uade.skillswap.application.usecases.publication.ListMyPublicationsService;
import com.uade.skillswap.application.usecases.publication.SearchPublicationsService;
import com.uade.skillswap.controllers.ControllerResult.ErrorType;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.publication.PublicationController;
import com.uade.skillswap.controllers.publication.PublicationControllerImpl;
import com.uade.skillswap.controllers.publication.PublicationForm;
import com.uade.skillswap.controllers.publication.SearchController;
import com.uade.skillswap.controllers.publication.SearchControllerImpl;
import com.uade.skillswap.controllers.publication.SearchForm;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.PublicationStatus;
import com.uade.skillswap.infrastructure.persistence.EntityManagerContext;
import com.uade.skillswap.infrastructure.persistence.JpaUnitOfWork;
import com.uade.skillswap.infrastructure.persistence.PersistenceConfiguration;
import com.uade.skillswap.infrastructure.persistence.catalog.JpaCategoryTree;
import com.uade.skillswap.infrastructure.persistence.publication.JpaPublicationRepository;
import com.uade.skillswap.infrastructure.persistence.publication.JpaPublicationSearchQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

// Integración de punta a punta (controller -> caso de uso -> JPA -> H2); solo se simulan perfil y login.
class PublicationIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant MONDAY = Instant.parse("2026-11-16T15:00:00Z");

    // Login simulado: el usuario activo que valida el controller.
    private static final AtomicReference<UserId> ACTIVE = new AtomicReference<>();

    private static PublicationController publications;
    private static SearchController search;

    @BeforeAll
    static void wireTheApplication() {
        PersistenceConfiguration h2 = new PersistenceConfiguration(
                "jdbc:h2:mem:publication-integration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        h2.migrate();
        EntityManagerContext.init(h2.createEntityManagerFactory());

        UnitOfWork unitOfWork = new JpaUnitOfWork();
        unitOfWork.execute(() -> EntityManagerContext.withEntityManager(em -> {
            em.createNativeQuery("INSERT INTO category (id, parent_id, name, kind) VALUES"
                    + " (1, NULL, 'Tecnología', 'CATEGORY'), (2, 1, 'Backend', 'CATEGORY')").executeUpdate();
            em.createNativeQuery("INSERT INTO profile (user_id, display_name, time_zone, account_type, reputation, created_at)"
                    + " VALUES ('ana', 'Ana', 'UTC', 'PROFESSIONAL', 4.50, '2026-11-14 10:00:00')").executeUpdate();
            return null;
        }));

        AuthorProfileQuery profiles = userId -> Optional.of(AccountType.PROFESSIONAL);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        JpaPublicationRepository repository = new JpaPublicationRepository();

        publications = new PublicationControllerImpl(
                new CreateDraftService(repository, profiles, unitOfWork, clock),
                new EditDraftService(repository, unitOfWork),
                new ChangePublicationStatusService(repository, unitOfWork, clock),
                new GetPublicationService(repository),
                new ListMyPublicationsService(repository),
                ACTIVE::get);
        search = new SearchControllerImpl(new SearchPublicationsService(new JpaPublicationSearchQuery(),
                new JpaCategoryTree()));
    }

    @BeforeEach
    void anaIsSignedIn() {
        ACTIVE.set(new UserId("ana"));
    }

    @AfterAll
    static void closeDatabase() {
        EntityManagerContext.close();
    }

    // Caso de aceptación 1: publicar una mentoría válida y encontrarla mediante búsqueda filtrada.
    @Test
    void case1_aValidMentoringIsPublishedAndFoundWithAFilteredSearch() {
        PublicationForm mentoring = new PublicationForm("MENTORING", "Kotlin interview prep",
                "Mock interviews for backend roles", 2L, "REMOTE", 60, 3, 120L, Set.of("kotlin", "interviews"),
                List.of(new PublicationForm.SlotForm(MONDAY, MONDAY.plus(Duration.ofHours(1)))));

        ControllerResult<PublicationView> draft = publications.createDraft("ana", mentoring);
        assertThat(draft.isSuccess()).isTrue();
        long id = draft.getData().id();

        ControllerResult<Page<PublicationSummary>> beforePublishing = search.search(new SearchForm("kotlin", null, null, null,
                null, null, null, null, null, null));
        assertThat(beforePublishing.getData().items()).isEmpty();

        ControllerResult<PublicationView> published = publications.changeStatus("ana", id, "PUBLISH", null);
        assertThat(published.isSuccess()).isTrue();
        assertThat(published.getData().status()).isEqualTo(PublicationStatus.PUBLISHED);

        ControllerResult<Page<PublicationSummary>> found = search.search(new SearchForm("kotlin", 1L, "REMOTE", 100L, 150L,
                "interviews", 4.0, "RELEVANCE", 0, 10));
        assertThat(found.isSuccess()).isTrue();
        assertThat(found.getData().items()).extracting(PublicationSummary::id).containsExactly(id);
        assertThat(found.getData().items().get(0).authorReputation()).isEqualTo(4.5);

        ControllerResult<Page<PublicationSummary>> filteredOut = search.search(new SearchForm("kotlin", null, "ON_SITE", null,
                null, null, null, null, null, null));
        assertThat(filteredOut.getData().items()).isEmpty();
    }

    // Caso de aceptación 2: rechazar la publicación de una oferta incompleta.
    @Test
    void case2_anIncompleteOfferIsRejectedAndStaysAsADraft() {
        PublicationForm incomplete = new PublicationForm("SERVICE", "Portfolio review", null, null, "REMOTE", null,
                1, null, null, List.of());
        long id = publications.createDraft("ana", incomplete).getData().id();

        ControllerResult<PublicationView> rejected = publications.changeStatus("ana", id, "PUBLISH", null);

        assertThat(rejected.isSuccess()).isFalse();
        assertThat(rejected.getErrorType()).isEqualTo(ErrorType.BUSINESS_RULE);
        assertThat(rejected.getMessage()).contains("category", "base price", "at least one time slot");
        assertThat(publications.getDetail("ana", id).getData().status()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(search.search(new SearchForm("portfolio", null, null, null, null, null, null, null, null, null))
                .getData().items()).isEmpty();
    }

    @Test
    void anInvalidTransitionAndSomeoneElsesDraftAreRejectedUniformly() {
        PublicationForm form = new PublicationForm("MENTORING", "Draft only", null, 1L, "REMOTE", null, 1, 10L,
                null, List.of(new PublicationForm.SlotForm(MONDAY, MONDAY.plus(Duration.ofHours(1)))));
        long id = publications.createDraft("ana", form).getData().id();

        assertThat(publications.changeStatus("ana", id, "RESUME", null).getErrorType())
                .isEqualTo(ErrorType.BUSINESS_RULE);
        assertThat(publications.changeStatus("bruno", id, "PUBLISH", null).getErrorType())
                .isEqualTo(ErrorType.ACCESS_DENIED);

        ACTIVE.set(new UserId("bruno"));
        assertThat(publications.changeStatus("bruno", id, "PUBLISH", null).getErrorType())
                .isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(publications.getDetail("bruno", id).getErrorType()).isEqualTo(ErrorType.NOT_FOUND);

        ACTIVE.set(new UserId("ana"));
        assertThat(publications.listMine("ana", "DRAFT", 0, 20).getData().items())
                .extracting(PublicationView::id).contains(id);
    }
}
