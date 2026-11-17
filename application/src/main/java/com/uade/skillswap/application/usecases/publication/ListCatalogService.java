package com.uade.skillswap.application.usecases.publication;

import com.uade.skillswap.application.common.*;
import com.uade.skillswap.application.dto.publication.CatalogEntryView;
import com.uade.skillswap.application.ports.in.publication.ListCatalogUseCase;
import com.uade.skillswap.application.ports.out.publication.CatalogQuery;
import java.util.Objects;

public final class ListCatalogService implements ListCatalogUseCase {
    private final CatalogQuery query;
    public ListCatalogService(CatalogQuery query) { this.query = Objects.requireNonNull(query); }
    @Override public Page<CatalogEntryView> list(PageRequest page) { return query.list(page); }
}
