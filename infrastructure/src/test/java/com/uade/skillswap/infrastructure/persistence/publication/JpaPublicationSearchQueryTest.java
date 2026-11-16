package com.uade.skillswap.infrastructure.persistence.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.publication.PublicationSummary;
import com.uade.skillswap.application.dto.publication.SearchFilters;
import com.uade.skillswap.application.dto.publication.SearchSort;
import com.uade.skillswap.application.usecases.publication.SearchPublicationsService;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.profile.AccountType;
import com.uade.skillswap.domain.publication.Modality;
import com.uade.skillswap.domain.publication.Publication;
import com.uade.skillswap.domain.publication.PublicationDetails;
import com.uade.skillswap.domain.publication.PublicationType;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import com.uade.skillswap.infrastructure.persistence.catalog.JpaCategoryTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Búsqueda contra H2, pasando por el caso de uso real con la consulta y el catálogo reales.
class JpaPublicationSearchQueryTest extends PersistenceTestBase {

    private static final UserId ANA = new UserId("ana");      // reputación 4.8
    private static final UserId BRUNO = new UserId("bruno");  // reputación 3.5
    private static final UserId CARLA = new UserId("carla");  // sin perfil: reputación 0
    private static final Instant DAY_1 = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant SLOT = Instant.parse("2026-12-01T15:00:00Z");

    private final JpaPublicationRepository repository = new JpaPublicationRepository();
    private final SearchPublicationsService search =
            new SearchPublicationsService(new JpaPublicationSearchQuery(), new JpaCategoryTree());

    @BeforeEach
    void catalogAndProfiles() {
        // Tecnología (1) > Backend (2) > Java (10, habilidad); Tecnología > Frontend (3); Diseño (5) > Fotografía (6)
        insertCategory(1, null, "Tecnología", "CATEGORY");
        insertCategory(2, 1, "Backend", "CATEGORY");
        insertCategory(10, 2, "Java", "SKILL");
        insertCategory(3, 1, "Frontend", "CATEGORY");
        insertCategory(5, null, "Diseño", "CATEGORY");
        insertCategory(6, 5, "Fotografía", "CATEGORY");

        // Perfiles es del área de Andy (#37); hasta que entre su migración, el test crea una tabla mínima.
        sql("CREATE TABLE IF NOT EXISTS perfiles (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id VARCHAR(100) NOT NULL,"
                + " nombre_visible VARCHAR(150) NOT NULL, tipo_cuenta VARCHAR(30) NOT NULL,"
                + " reputacion DECIMAL(3, 2) NOT NULL DEFAULT 0.00, activo BOOLEAN NOT NULL DEFAULT TRUE)");
        insertProfile("ana", 4.8);
        insertProfile("bruno", 3.5);
    }

    private void insertCategory(long id, Integer parent, String name, String kind) {
        sql("INSERT INTO category (id, parent_id, name, kind) VALUES (?, ?, ?, ?)", id, parent, name, kind);
    }

    private void insertProfile(String userId, double reputation) {
        sql("INSERT INTO perfiles (user_id, nombre_visible, tipo_cuenta, reputacion) VALUES (?, ?, ?, ?)",
                userId, userId, "PROFESIONAL", reputation);
    }

    private long publish(UserId author, String title, String description, long category, Modality modality,
                         long price, Instant publishedAt, String... tags) {
        Publication publication = Publication.draft(author, AccountType.PROFESSIONAL, PublicationType.MENTORING,
                new PublicationDetails(title, description, category, modality, Duration.ofHours(1), 2,
                        new SkillCoins(price), Set.of(tags)), publishedAt);
        publication.addSlot(author, SLOT, SLOT.plus(Duration.ofHours(1)));
        publication.addSlot(author, SLOT.plus(Duration.ofDays(1)), SLOT.plus(Duration.ofDays(1)).plusSeconds(3600));
        publication.publish(author, publishedAt);
        return unitOfWork.execute(() -> repository.save(publication)).getId();
    }

    private Page<PublicationSummary> find(String text, Long category, Modality modality, Long min, Long max,
                                          String skill, Double reputation, SearchSort sort, PageRequest page) {
        return search.search(new SearchFilters(text, category, modality, min, max, skill, reputation, sort, page));
    }

    // Caso de aceptación 1: publicar una mentoría válida y encontrarla con búsqueda filtrada.
    @Test
    void aPublishedMentoringIsFoundWithEveryFilterCombined() {
        long mentoring = publish(ANA, "Mock interview in Java", "Practice for backend interviews", 2,
                Modality.REMOTE, 100, DAY_1, "java", "interviews");
        publish(BRUNO, "Mock interview in Java", "Same but on site", 2, Modality.ON_SITE, 100, DAY_1, "java");
        publish(ANA, "Portrait session", "Studio photography", 6, Modality.ON_SITE, 300, DAY_1, "photo");

        Page<PublicationSummary> found = find("interview", 1L, Modality.REMOTE, 50L, 150L, "java", 4.0,
                SearchSort.RELEVANCE, PageRequest.first());

        assertThat(found.items()).extracting(PublicationSummary::id).containsExactly(mentoring);
        PublicationSummary row = found.items().get(0);
        assertThat(row.authorId()).isEqualTo("ana");
        assertThat(row.authorReputation()).isEqualTo(4.8);
        assertThat(row.basePrice()).isEqualTo(100);
        assertThat(row.availableSpots()).isEqualTo(2);
        assertThat(row.tags()).containsExactlyInAnyOrder("java", "interviews");
        assertThat(row.publishedAt()).isEqualTo(DAY_1);
    }

    @Test
    void onlyPublishedPublicationsAreFound() {
        long visible = publish(ANA, "Visible", null, 2, Modality.REMOTE, 10, DAY_1);
        long paused = publish(ANA, "Paused", null, 2, Modality.REMOTE, 10, DAY_1);
        unitOfWork.execute(() -> {
            Publication publication = repository.findById(paused).orElseThrow();
            publication.pause(ANA, DAY_1);
            return repository.save(publication);
        });

        assertThat(find(null, null, null, null, null, null, null, null, null).items())
                .extracting(PublicationSummary::id).containsExactly(visible);
    }

    @Test
    void aCategoryIncludesItsSubcategoriesAndSkills() {
        long backend = publish(ANA, "Backend", null, 2, Modality.REMOTE, 10, DAY_1);
        long java = publish(ANA, "Java", null, 10, Modality.REMOTE, 10, DAY_1);
        long frontend = publish(ANA, "Frontend", null, 3, Modality.REMOTE, 10, DAY_1);
        publish(ANA, "Photo", null, 6, Modality.REMOTE, 10, DAY_1);

        assertThat(find(null, 1L, null, null, null, null, null, null, null).items())
                .extracting(PublicationSummary::id).containsExactlyInAnyOrder(backend, java, frontend);
        assertThat(find(null, 2L, null, null, null, null, null, null, null).items())
                .extracting(PublicationSummary::id).containsExactlyInAnyOrder(backend, java);
        assertThatThrownBy(() -> find(null, 99L, null, null, null, null, null, null, null))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found with id 99");
    }

    @Test
    void relevanceWeighsTitleOverTagsOverDescription() {
        long inDescription = publish(ANA, "Career talk", "We talk about spring and more", 1, Modality.REMOTE, 10, DAY_1);
        long inTag = publish(ANA, "Backend mentoring", null, 1, Modality.REMOTE, 10, DAY_1, "spring");
        long inTitle = publish(ANA, "Spring Boot from scratch", null, 1, Modality.REMOTE, 10, DAY_1);
        long inTitleAndTag = publish(ANA, "Advanced Spring", null, 1, Modality.REMOTE, 10, DAY_1, "spring");
        publish(ANA, "Unrelated", null, 1, Modality.REMOTE, 10, DAY_1);

        assertThat(find("SPRING", null, null, null, null, null, null, SearchSort.RELEVANCE, null).items())
                .extracting(PublicationSummary::id).containsExactly(inTitleAndTag, inTitle, inTag, inDescription);
    }

    @Test
    void equalRelevanceIsBrokenByReputation() {
        long bruno = publish(BRUNO, "Java review", null, 1, Modality.REMOTE, 10, DAY_1);
        long ana = publish(ANA, "Java review", null, 1, Modality.REMOTE, 10, DAY_1);

        assertThat(find("java", null, null, null, null, null, null, SearchSort.RELEVANCE, null).items())
                .extracting(PublicationSummary::id).containsExactly(ana, bruno);
    }

    @Test
    void theResultsAreSortedByReputationPriceOrDate() {
        long cheapOld = publish(BRUNO, "A", null, 1, Modality.REMOTE, 10, DAY_1);
        long expensiveNew = publish(ANA, "B", null, 1, Modality.REMOTE, 90, DAY_1.plus(Duration.ofDays(2)));
        long noProfileMiddle = publish(CARLA, "C", null, 1, Modality.REMOTE, 50, DAY_1.plus(Duration.ofDays(1)));

        assertThat(find(null, null, null, null, null, null, null, SearchSort.BEST_REPUTATION, null).items())
                .extracting(PublicationSummary::id).containsExactly(expensiveNew, cheapOld, noProfileMiddle);
        assertThat(find(null, null, null, null, null, null, null, SearchSort.LOWEST_PRICE, null).items())
                .extracting(PublicationSummary::id).containsExactly(cheapOld, noProfileMiddle, expensiveNew);
        assertThat(find(null, null, null, null, null, null, null, SearchSort.MOST_RECENT, null).items())
                .extracting(PublicationSummary::id).containsExactly(expensiveNew, noProfileMiddle, cheapOld);
    }

    @Test
    void anAuthorWithoutProfileCountsAsReputationZero() {
        publish(CARLA, "No profile", null, 1, Modality.REMOTE, 10, DAY_1);

        assertThat(find(null, null, null, null, null, null, 0.5, null, null).items()).isEmpty();
        assertThat(find(null, null, null, null, null, null, null, null, null).items().get(0).authorReputation())
                .isZero();
    }

    @Test
    void theOffersOfAnInactiveProfileAreNotFound() {
        long active = publish(ANA, "Active author", null, 1, Modality.REMOTE, 10, DAY_1);
        publish(BRUNO, "Inactive author", null, 1, Modality.REMOTE, 10, DAY_1);
        sql("UPDATE perfiles SET activo = FALSE WHERE user_id = ?", "bruno");

        assertThat(find(null, null, null, null, null, null, null, null, null).items())
                .extracting(PublicationSummary::id).containsExactly(active);
    }

    @Test
    void theResultsArePaginated() {
        for (int i = 0; i < 5; i++) {
            publish(ANA, "Mentoring " + i, null, 1, Modality.REMOTE, 10 + i, DAY_1);
        }

        Page<PublicationSummary> second = find(null, null, null, null, null, null, null, SearchSort.LOWEST_PRICE,
                new PageRequest(1, 2));

        assertThat(second.items()).extracting(PublicationSummary::basePrice).containsExactly(12L, 13L);
        assertThat(second.totalItems()).isEqualTo(5);
        assertThat(second.totalPages()).isEqualTo(3);
        assertThat(second.hasNext()).isTrue();
    }

    @Test
    void theSearchTextIsLiteralAndWildcardsDoNotMatchEverything() {
        long percent = publish(ANA, "100% practical", null, 1, Modality.REMOTE, 10, DAY_1);
        publish(ANA, "Theory only", null, 1, Modality.REMOTE, 10, DAY_1);

        assertThat(find("%", null, null, null, null, null, null, null, null).items())
                .extracting(PublicationSummary::id).containsExactly(percent);
    }

    @Test
    void theAvailableSpotsDiscountTheOccupiedSlots() {
        long id = publish(ANA, "Busy", null, 1, Modality.REMOTE, 10, DAY_1);
        unitOfWork.execute(() -> {
            Publication publication = repository.findById(id).orElseThrow();
            publication.reserve(publication.getSlots().get(0));
            return repository.save(publication);
        });

        assertThat(find(null, null, null, null, null, null, null, null, null).items().get(0).availableSpots())
                .isEqualTo(1);
    }
}
