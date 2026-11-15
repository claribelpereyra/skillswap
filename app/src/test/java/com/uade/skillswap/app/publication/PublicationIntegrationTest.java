package com.uade.skillswap.app.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.ports.in.publication.PublicationSummary;
import com.uade.skillswap.application.ports.in.publication.PublicationView;
import com.uade.skillswap.application.ports.out.TimeProvider;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.publication.AuthorProfileQuery;
import com.uade.skillswap.application.usecases.publication.ChangePublicationStatusService;
import com.uade.skillswap.application.usecases.publication.CreateDraftService;
import com.uade.skillswap.application.usecases.publication.EditDraftService;
import com.uade.skillswap.application.usecases.publication.GetPublicationService;
import com.uade.skillswap.application.usecases.publication.ListMyPublicationsService;
import com.uade.skillswap.application.usecases.publication.SearchPublicationsService;
import com.uade.skillswap.controllers.common.ErrorCode;
import com.uade.skillswap.controllers.common.Result;
import com.uade.skillswap.controllers.publication.PublicationController;
import com.uade.skillswap.controllers.publication.PublicationControllerImpl;
import com.uade.skillswap.controllers.publication.PublicationForm;
import com.uade.skillswap.controllers.publication.SearchController;
import com.uade.skillswap.controllers.publication.SearchControllerImpl;
import com.uade.skillswap.controllers.publication.SearchForm;
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
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Integración de punta a punta del área de publicaciones: controller -> caso de uso -> JPA -> H2 con las
// migraciones reales. Lo único simulado es el perfil del autor (puerto del área de perfiles).
class PublicationIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant MONDAY = Instant.parse("2026-11-16T15:00:00Z");

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
            em.createNativeQuery("INSERT INTO categories (id, parent_id, name, kind) VALUES"
                    + " (1, NULL, 'Tecnología', 'CATEGORY'), (2, 1, 'Backend', 'CATEGORY')").executeUpdate();
            // Tabla de perfiles del área de Andy (ver JpaPublicationSearchQuery); se crea si su migración no está.
            em.createNativeQuery("CREATE TABLE IF NOT EXISTS perfiles (id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + " user_id VARCHAR(100) NOT NULL, nombre_visible VARCHAR(150) NOT NULL,"
                    + " tipo_cuenta VARCHAR(30) NOT NULL, reputacion DECIMAL(3, 2) NOT NULL DEFAULT 0.00,"
                    + " activo BOOLEAN NOT NULL DEFAULT TRUE)")
                    .executeUpdate();
            em.createNativeQuery("INSERT INTO perfiles (user_id, nombre_visible, tipo_cuenta, reputacion)"
                    + " VALUES ('ana', 'Ana', 'PROFESIONAL', 4.50)").executeUpdate();
            return null;
        }));

        AuthorProfileQuery profiles = userId -> Optional.of(AccountType.PROFESSIONAL);
        TimeProvider timeProvider = () -> NOW;
        JpaPublicationRepository repository = new JpaPublicationRepository();

        publications = new PublicationControllerImpl(
                new CreateDraftService(repository, profiles, unitOfWork, timeProvider),
                new EditDraftService(repository, unitOfWork),
                new ChangePublicationStatusService(repository, unitOfWork, timeProvider),
                new GetPublicationService(repository),
                new ListMyPublicationsService(repository));
        search = new SearchControllerImpl(new SearchPublicationsService(new JpaPublicationSearchQuery(),
                new JpaCategoryTree()));
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

        Result<PublicationView> draft = publications.createDraft("ana", mentoring);
        assertThat(draft.isOk()).isTrue();
        long id = draft.value().id();

        Result<Page<PublicationSummary>> beforePublishing = search.search(new SearchForm("kotlin", null, null, null,
                null, null, null, null, null, null));
        assertThat(beforePublishing.value().items()).isEmpty();

        Result<PublicationView> published = publications.changeStatus("ana", id, "PUBLISH", null);
        assertThat(published.isOk()).isTrue();
        assertThat(published.value().status()).isEqualTo(PublicationStatus.PUBLISHED);

        Result<Page<PublicationSummary>> found = search.search(new SearchForm("kotlin", 1L, "REMOTE", 100L, 150L,
                "interviews", 4.0, "RELEVANCE", 0, 10));
        assertThat(found.isOk()).isTrue();
        assertThat(found.value().items()).extracting(PublicationSummary::id).containsExactly(id);
        assertThat(found.value().items().get(0).authorReputation()).isEqualTo(4.5);

        Result<Page<PublicationSummary>> filteredOut = search.search(new SearchForm("kotlin", null, "ON_SITE", null,
                null, null, null, null, null, null));
        assertThat(filteredOut.value().items()).isEmpty();
    }

    // Caso de aceptación 2: rechazar la publicación de una oferta incompleta.
    @Test
    void case2_anIncompleteOfferIsRejectedAndStaysAsADraft() {
        PublicationForm incomplete = new PublicationForm("SERVICE", "Portfolio review", null, null, "REMOTE", null,
                1, null, null, List.of());
        long id = publications.createDraft("ana", incomplete).value().id();

        Result<PublicationView> rejected = publications.changeStatus("ana", id, "PUBLISH", null);

        assertThat(rejected.isOk()).isFalse();
        assertThat(rejected.error().code()).isEqualTo(ErrorCode.BUSINESS_RULE);
        assertThat(rejected.error().message()).contains("category", "base price", "at least one time slot");
        assertThat(publications.getDetail("ana", id).value().status()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(search.search(new SearchForm("portfolio", null, null, null, null, null, null, null, null, null))
                .value().items()).isEmpty();
    }

    @Test
    void anInvalidTransitionAndSomeoneElsesDraftAreRejectedUniformly() {
        PublicationForm form = new PublicationForm("MENTORING", "Draft only", null, 1L, "REMOTE", null, 1, 10L,
                null, List.of(new PublicationForm.SlotForm(MONDAY, MONDAY.plus(Duration.ofHours(1)))));
        long id = publications.createDraft("ana", form).value().id();

        assertThat(publications.changeStatus("ana", id, "RESUME", null).error().code())
                .isEqualTo(ErrorCode.INVALID_TRANSITION);
        assertThat(publications.changeStatus("bruno", id, "PUBLISH", null).error().code())
                .isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(publications.getDetail("bruno", id).error().code()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(publications.listMine("ana", "DRAFT", 0, 20).value().items())
                .extracting(PublicationView::id).contains(id);
    }
}
