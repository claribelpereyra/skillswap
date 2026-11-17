package com.uade.skillswap.application.ports.out.publication;

import com.uade.skillswap.application.common.*;
import com.uade.skillswap.application.dto.publication.CatalogEntryView;

public interface CatalogQuery {
    Page<CatalogEntryView> list(PageRequest page);
}
