package com.uade.skillswap.controllers.publication;

import com.uade.skillswap.application.common.*;
import com.uade.skillswap.application.dto.publication.CatalogEntryView;
import com.uade.skillswap.application.ports.in.publication.ListCatalogUseCase;
import com.uade.skillswap.controllers.ControllerResult;
import java.util.Objects;

public final class CatalogControllerImpl implements CatalogController {
    private final ListCatalogUseCase query;
    public CatalogControllerImpl(ListCatalogUseCase query) { this.query = Objects.requireNonNull(query); }
    @Override public ControllerResult<Page<CatalogEntryView>> list(int page, int size) {
        return ControllerResult.of(() -> query.list(new PageRequest(page, size)));
    }
}
