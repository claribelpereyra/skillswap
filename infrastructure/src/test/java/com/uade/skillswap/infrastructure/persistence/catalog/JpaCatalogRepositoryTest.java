package com.uade.skillswap.infrastructure.persistence.catalog;

import com.uade.skillswap.application.usecases.publication.ManageCatalogService;
import com.uade.skillswap.infrastructure.persistence.PersistenceTestBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JpaCatalogRepositoryTest extends PersistenceTestBase {

    private final JpaCategoryTree catalog = new JpaCategoryTree();
    private final ManageCatalogService service = new ManageCatalogService(catalog, unitOfWork);

    @Test
    void addedItemsArePersistedAsATreeAndCanBeFoundByTheDraftValidation() {
        long technology = service.addCategory(null, "Technology").id();
        long backend = service.addCategory(technology, "Backend").id();
        long java = service.addSkill(backend, "Java").id();

        assertThat(count("category")).isEqualTo(3);
        assertThat(unitOfWork.execute(() -> catalog.contains(java))).isTrue();
        assertThat(unitOfWork.execute(() -> catalog.contains(999))).isFalse();
        assertThat(unitOfWork.execute(() -> catalog.idsUnder(technology)))
                .hasValueSatisfying(ids -> assertThat(ids).containsExactlyInAnyOrder(technology, backend, java));
        assertThat(unitOfWork.execute(() -> catalog.loadAll().get(backend).getParent().getId()))
                .isEqualTo(technology);
    }
}
