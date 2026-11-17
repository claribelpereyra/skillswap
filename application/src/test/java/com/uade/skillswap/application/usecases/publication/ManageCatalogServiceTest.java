package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.publication.CatalogEntryView;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.publication.CatalogRepository;
import com.uade.skillswap.domain.catalog.CatalogItem;
import com.uade.skillswap.domain.catalog.Category;
import com.uade.skillswap.domain.catalog.Skill;
import com.uade.skillswap.domain.common.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManageCatalogServiceTest {

    private final InMemoryCatalog catalog = new InMemoryCatalog();
    private final UnitOfWork unitOfWork = Supplier::get;
    private final ManageCatalogService service = new ManageCatalogService(catalog, unitOfWork);

    @Test
    void addsTopLevelCategoriesSubcategoriesAndSkills() {
        CatalogEntryView technology = service.addCategory(null, "Technology");
        CatalogEntryView backend = service.addCategory(technology.id(), "Backend");
        CatalogEntryView java = service.addSkill(backend.id(), "Java");

        assertThat(technology).isEqualTo(new CatalogEntryView(1, "Technology", "CATEGORY"));
        assertThat(java).isEqualTo(new CatalogEntryView(3, "Java", "SKILL"));
        assertThat(catalog.saved).containsExactly("null>Technology", "1>Backend", "2>Java");
        assertThat(catalog.loadAll().get(technology.id()).idsInSubtree())
                .containsExactlyInAnyOrder(technology.id(), backend.id(), java.id());
    }

    @Test
    void theCompositeRejectsRepeatedNamesAndSkillsAsParents() {
        CatalogEntryView technology = service.addCategory(null, "Technology");
        CatalogEntryView java = service.addSkill(technology.id(), "Java");

        assertThatThrownBy(() -> service.addSkill(technology.id(), "java"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.addCategory(null, "TECHNOLOGY"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.addSkill(java.id(), "Spring"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A skill cannot contain other catalog items");
        assertThatThrownBy(() -> service.addSkill(99, "Spring"))
                .isExactlyInstanceOf(ResourceNotFoundException.class);
        assertThat(catalog.saved).hasSize(2);
    }

    private static final class InMemoryCatalog implements CatalogRepository {
        private final Map<Long, Long> parents = new HashMap<>();
        private final Map<Long, String> kinds = new HashMap<>();
        private final Map<Long, String> names = new HashMap<>();
        private final List<String> saved = new ArrayList<>();

        @Override
        public Map<Long, CatalogItem> loadAll() {
            Map<Long, CatalogItem> items = new HashMap<>();
            names.forEach((id, name) -> items.put(id, kinds.get(id).equals("SKILL") ? new Skill(id, name)
                    : new Category(id, name)));
            parents.forEach((id, parent) -> {
                if (parent != null) {
                    ((Category) items.get(parent)).add(items.get(id));
                }
            });
            return items;
        }

        @Override
        public long nextId() {
            return names.size() + 1L;
        }

        @Override
        public void add(Long parentId, CatalogItem item) {
            parents.put(item.getId(), parentId);
            kinds.put(item.getId(), item instanceof Skill ? "SKILL" : "CATEGORY");
            names.put(item.getId(), item.getName());
            saved.add(parentId + ">" + item.getName());
        }
    }
}
