package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.dto.publication.CatalogEntryView;
import com.uade.skillswap.controllers.ControllerResult;

public interface CatalogController {
    ControllerResult<Page<CatalogEntryView>> list(int page, int size);

    ControllerResult<CatalogEntryView> addCategory(String userId, Long parentId, String name);

    ControllerResult<CatalogEntryView> addSkill(String userId, long categoryId, String name);
}
