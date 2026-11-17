package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.publication.CatalogEntryView;
import com.uade.skillswap.application.ports.in.publication.ManageCatalogUseCase;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.publication.CatalogRepository;
import com.uade.skillswap.domain.catalog.CatalogItem;
import com.uade.skillswap.domain.catalog.Category;
import com.uade.skillswap.domain.catalog.Skill;
import com.uade.skillswap.domain.common.BusinessRuleException;

import java.util.Map;
import java.util.Objects;

// Amplía el catálogo: el Composite (Category.add) valida ciclos, padres únicos y nombres repetidos entre hermanos.
public final class ManageCatalogService implements ManageCatalogUseCase {

    private final CatalogRepository catalog;
    private final UnitOfWork unitOfWork;

    public ManageCatalogService(CatalogRepository catalog, UnitOfWork unitOfWork) {
        this.catalog = Objects.requireNonNull(catalog);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
    }

    @Override
    public CatalogEntryView addCategory(Long parentId, String name) {
        return unitOfWork.execute(() -> {
            Map<Long, CatalogItem> items = catalog.loadAll();
            Category category = new Category(catalog.nextId(), name);
            if (parentId == null) {
                requireUniqueRootName(items, category);
            } else {
                parentCategory(items, parentId).add(category);
            }
            catalog.add(parentId, category);
            return new CatalogEntryView(category.getId(), category.getName(), "CATEGORY");
        });
    }

    @Override
    public CatalogEntryView addSkill(long categoryId, String name) {
        return unitOfWork.execute(() -> {
            Map<Long, CatalogItem> items = catalog.loadAll();
            Skill skill = new Skill(catalog.nextId(), name);
            parentCategory(items, categoryId).add(skill);
            catalog.add(categoryId, skill);
            return new CatalogEntryView(skill.getId(), skill.getName(), "SKILL");
        });
    }

    private static Category parentCategory(Map<Long, CatalogItem> items, long parentId) {
        CatalogItem parent = items.get(parentId);
        if (parent == null) {
            throw new ResourceNotFoundException("Category", parentId);
        }
        if (!(parent instanceof Category category)) {
            throw new BusinessRuleException("A skill cannot contain other catalog items");
        }
        return category;
    }

    private static void requireUniqueRootName(Map<Long, CatalogItem> items, Category category) {
        boolean repeated = items.values().stream()
                .filter(item -> item.getParent() == null)
                .anyMatch(item -> item.getName().equalsIgnoreCase(category.getName()));
        if (repeated) {
            throw new BusinessRuleException("There is already a top-level category named " + category.getName());
        }
    }
}
