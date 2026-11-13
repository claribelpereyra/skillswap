package com.uade.skillswap.application.common;

import java.util.List;
import java.util.function.Function;

public record Page<T>(List<T> items, PageRequest request, long totalItems) {

    public Page {
        items = List.copyOf(items);
        if (items.size() > request.size()) {
            throw new IllegalArgumentException("A page cannot hold more items than its size");
        }
        if (totalItems < request.offset() + items.size()) {
            throw new IllegalArgumentException("Total items cannot be lower than the items already listed");
        }
    }

    public static <T> Page<T> empty(PageRequest request) {
        return new Page<>(List.of(), request, 0);
    }

    public long totalPages() {
        return (totalItems + request.size() - 1) / request.size();
    }

    public boolean hasNext() {
        return request.number() + 1 < totalPages();
    }

    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        return new Page<>(items.stream().<R>map(mapper).toList(), request, totalItems);
    }
}
