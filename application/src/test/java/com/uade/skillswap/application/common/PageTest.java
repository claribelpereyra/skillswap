package com.uade.skillswap.application.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageTest {

    @Test
    void computesTheOffsetOfThePage() {
        assertThat(new PageRequest(2, 10).offset()).isEqualTo(20);
        assertThat(PageRequest.first().offset()).isZero();
    }

    @Test
    void doesNotAllowNegativePagesOrSizesOutOfRange() {
        assertThatThrownBy(() -> new PageRequest(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageRequest(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageRequest(0, PageRequest.MAX_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void computesTotalPagesAndWhetherThereIsANextPage() {
        Page<String> first = new Page<>(List.of("a", "b"), new PageRequest(0, 2), 5);
        Page<String> last = new Page<>(List.of("e"), new PageRequest(2, 2), 5);

        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.hasNext()).isTrue();
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    void anEmptyPageHasNoNextPage() {
        Page<String> empty = Page.empty(PageRequest.first());

        assertThat(empty.items()).isEmpty();
        assertThat(empty.totalPages()).isZero();
        assertThat(empty.hasNext()).isFalse();
    }

    @Test
    void mapsTheItemsKeepingThePagination() {
        Page<Integer> numbers = new Page<>(List.of(1, 2), new PageRequest(1, 2), 6);

        Page<String> texts = numbers.map(n -> "#" + n);

        assertThat(texts.items()).containsExactly("#1", "#2");
        assertThat(texts.request()).isEqualTo(numbers.request());
        assertThat(texts.totalItems()).isEqualTo(6);
    }

    @Test
    void isNotAffectedWhenTheOriginalListChanges() {
        List<String> original = new ArrayList<>(List.of("a"));
        Page<String> page = new Page<>(original, PageRequest.first(), 1);

        original.add("b");

        assertThat(page.items()).containsExactly("a");
    }

    @Test
    void rejectsInconsistentData() {
        assertThatThrownBy(() -> new Page<>(List.of("a", "b", "c"), new PageRequest(0, 2), 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Page<>(List.of("a"), new PageRequest(3, 2), 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
