package com.uade.skillswap.application.ports.out.publication;

import com.uade.skillswap.domain.catalog.CatalogItem;

import java.util.Map;

// Puerto de salida para leer el catálogo armado como Composite y agregarle categorías o habilidades.
public interface CatalogRepository {

    Map<Long, CatalogItem> loadAll();

    long nextId();

    void add(Long parentId, CatalogItem item);
}
