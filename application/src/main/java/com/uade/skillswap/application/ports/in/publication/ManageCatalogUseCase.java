package com.uade.skillswap.application.ports.in.publication;

import com.uade.skillswap.application.dto.publication.CatalogEntryView;

public interface ManageCatalogUseCase {

    CatalogEntryView addCategory(Long parentId, String name);

    CatalogEntryView addSkill(long categoryId, String name);
}
