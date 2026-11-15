package com.uade.skillswap.domain.catalog;

import com.uade.skillswap.domain.common.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogTest {

    // Tecnología
    // ├── Backend
    // │   ├── Java
    // │   └── Bases de datos
    // │       └── MySQL
    // └── Frontend
    //     └── React
    private Category technology;
    private Category backend;
    private Category databases;
    private Skill java;
    private Skill mysql;
    private Skill react;

    @BeforeEach
    void buildCatalog() {
        java = new Skill(10, "Java");
        mysql = new Skill(11, "MySQL");
        react = new Skill(12, "React");
        databases = new Category(3, "Bases de datos").add(mysql);
        backend = new Category(2, "Backend").add(java).add(databases);
        technology = new Category(1, "Tecnología")
                .add(backend)
                .add(new Category(4, "Frontend").add(react));
    }

    @Test
    void aCategoryIncludesItsSubcategoriesAndSkillsAtAnyDepth() {
        assertThat(technology.idsInSubtree()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 10L, 11L, 12L);
        assertThat(backend.idsInSubtree()).containsExactlyInAnyOrder(2L, 3L, 10L, 11L);
        assertThat(technology.includes(11)).isTrue();
        assertThat(backend.includes(12)).isFalse();
    }

    @Test
    void aSkillIsTraversedLikeACategory() {
        CatalogItem leaf = java;
        CatalogItem composite = backend;

        assertThat(leaf.idsInSubtree()).containsExactly(10L);
        assertThat(leaf.skills()).containsExactly(java);
        assertThat(composite.skills()).containsExactly(java, mysql);
    }

    @Test
    void theSkillsOfACategoryAreCollectedFromEveryLevel() {
        assertThat(technology.skills()).containsExactly(java, mysql, react);
        assertThat(new Category(5, "Diseño").skills()).isEmpty();
    }

    @Test
    void anItemIsFoundByIdAnywhereInTheTree() {
        assertThat(technology.find(11)).contains(mysql);
        assertThat(technology.find(3)).contains(databases);
        assertThat(technology.find(1)).contains(technology);
        assertThat(technology.find(99)).isEmpty();
        assertThat(java.find(11)).isEmpty();
    }

    @Test
    void aCategoryCannotContainItselfNorAnAncestor() {
        assertThatThrownBy(() -> technology.add(technology)).isInstanceOf(BusinessRuleException.class)
                .hasMessage("A category cannot contain itself");
        assertThatThrownBy(() -> databases.add(technology)).isInstanceOf(BusinessRuleException.class)
                .hasMessage("A category cannot contain itself");
    }

    @Test
    void anItemCannotBeAddedTwiceToTheSameTree() {
        assertThatThrownBy(() -> technology.add(new Skill(11, "MySQL 8")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("already contains");
        assertThatThrownBy(() -> technology.add(new Category(99, "Otra").add(new Skill(10, "Java 17"))))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("already contains");
    }

    @Test
    void anItemOfAnotherBranchCannotBeAddedAgain() {
        assertThatThrownBy(() -> backend.add(react))
                .isInstanceOf(BusinessRuleException.class).hasMessage("React already belongs to Frontend");
        assertThatThrownBy(() -> backend.add(new Skill(12, "React JS")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("already contains");
        assertThat(technology.skills()).containsExactly(java, mysql, react);
    }

    @Test
    void eachItemKnowsItsParent() {
        assertThat(mysql.getParent()).isEqualTo(databases);
        assertThat(databases.getParent()).isEqualTo(backend);
        assertThat(technology.getParent()).isNull();
    }

    @Test
    void twoSiblingsCannotHaveTheSameName() {
        assertThatThrownBy(() -> backend.add(new Skill(20, " java ")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("There is already an item named java in Backend");
    }

    @Test
    void anItemNeedsAName() {
        assertThatThrownBy(() -> new Skill(30, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Category(31, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theChildrenOfACategoryCannotBeModifiedFromOutside() {
        assertThatThrownBy(() -> backend.getChildren().add(react)).isInstanceOf(UnsupportedOperationException.class);
    }
}
