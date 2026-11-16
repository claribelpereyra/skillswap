package com.uade.skillswap.application.ports.in.publication;

import com.uade.skillswap.application.common.*;
import com.uade.skillswap.application.dto.publication.CatalogEntryView;

public interface ListCatalogUseCase {
    Page<CatalogEntryView> list(PageRequest page);
}
