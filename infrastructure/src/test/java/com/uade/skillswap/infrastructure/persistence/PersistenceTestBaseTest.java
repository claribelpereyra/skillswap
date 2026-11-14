package com.uade.skillswap.infrastructure.persistence;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Prueba la base de tests de persistencia que usan todas las áreas.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PersistenceTestBaseTest extends PersistenceTestBase {

    @Test
    @Order(1)
    void theMigrationsAreAppliedToTheTestDatabase() {
        long applied = inTransaction(em -> ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE").getSingleResult()).longValue());

        assertThat(applied).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(2)
    void eachTestStartsWithEmptyTablesEvenIfMigrationsInsertedData() {
        assertThat(count("info_esquema")).isZero();

        sql("INSERT INTO info_esquema (clave, valor) VALUES (?, ?)", "left-by-test-2", "value");
        assertThat(count("info_esquema")).isEqualTo(1);
    }

    @Test
    @Order(3)
    void whatAPreviousTestLeftIsGone() {
        assertThat(count("info_esquema")).isZero();
    }

    @Test
    @Order(4)
    void sqlAcceptsPlainOrNumberedParameters() {
        sql("INSERT INTO info_esquema (clave, valor) VALUES (?, ?)", "a", "1");
        sql("INSERT INTO info_esquema (clave, valor) VALUES (?2, ?1)", "2", "b");

        String value = inTransaction(em -> (String) em.createNativeQuery(
                "SELECT valor FROM info_esquema WHERE clave = 'b'").getSingleResult());
        assertThat(value).isEqualTo("2");
        assertThat(count("info_esquema")).isEqualTo(2);
    }

    @Test
    @Order(5)
    void aFailedTransactionLeavesNothingBehind() {
        assertThatThrownBy(() -> inTransaction(em -> {
            em.createNativeQuery("INSERT INTO info_esquema (clave, valor) VALUES ('x', 'y')").executeUpdate();
            throw new IllegalStateException("failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(count("info_esquema")).isZero();
    }
}
