package com.uade.skillswap.infraestructura.persistencia.notificaciones;

import com.uade.skillswap.dominio.notificaciones.*;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class RepositoriosNotificacionesTest {
    private EntityManagerFactory fabrica;
    private RepositorioNotificacionesJpa avisos;
    private RepositorioPreferenciasJpa preferencias;

    @BeforeEach void preparar() {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").load().migrate();
        fabrica = new Configuration().addAnnotatedClass(NotificacionJpaEntity.class)
                .addAnnotatedClass(PreferenciaJpaEntity.class)
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", "sa")
                .setProperty("hibernate.connection.password", "")
                .setProperty("hibernate.hbm2ddl.auto", "validate")
                .setProperty("hibernate.jdbc.time_zone", "UTC")
                .buildSessionFactory();
        avisos = new RepositorioNotificacionesJpa(fabrica);
        preferencias = new RepositorioPreferenciasJpa(fabrica);
    }

    @AfterEach void cerrar() { if (fabrica != null) fabrica.close(); }

    private Notificacion aviso(String usuario, Instant fecha) {
        return new Notificacion(UUID.randomUUID(), new Aviso(UUID.randomUUID(), fecha,
                TipoEventoNotificable.PAGO_CONFIRMADO, "contratacion-1", usuario, "Pago confirmado"),
                CanalPreferido.EMAIL, EstadoEntrega.PENDIENTE, null);
    }

    @Test void guardaYActualizaPreferencia() {
        assertThat(preferencias.buscar("ana")).isEmpty();
        preferencias.guardar(new PreferenciaNotificacion("ana", CanalPreferido.EMAIL));
        preferencias.guardar(new PreferenciaNotificacion("ana", CanalPreferido.PUSH));
        assertThat(preferencias.buscar("ana")).contains(new PreferenciaNotificacion("ana", CanalPreferido.PUSH));
    }

    @Test void paginaFiltraOrdenaYConservaPrimeraLectura() {
        var viejo = aviso("ana", Instant.EPOCH);
        var nuevo = aviso("ana", Instant.EPOCH.plusSeconds(60));
        avisos.registrarSiAusente(viejo);
        avisos.registrarSiAusente(nuevo);
        avisos.registrarSiAusente(aviso("otro", Instant.EPOCH));
        assertThat(avisos.listar("ana", 0, 1).elementos()).containsExactly(nuevo);
        assertThat(avisos.listar("ana", 1, 1).elementos()).containsExactly(viejo);
        assertThat(avisos.listar("ana", 2, 1).elementos()).isEmpty();
        assertThat(avisos.listar("ana", 0, 1).total()).isEqualTo(2);
        assertThat(avisos.marcarLeida("otro", nuevo.id(), Instant.EPOCH)).isFalse();
        assertThat(avisos.marcarLeida("ana", nuevo.id(), Instant.EPOCH.plusSeconds(90))).isTrue();
        assertThat(avisos.marcarLeida("ana", nuevo.id(), Instant.EPOCH.plusSeconds(100))).isTrue();
        avisos.registrarEntrega("ana", nuevo.id(), EstadoEntrega.ENVIADA);
        var leida = avisos.listar("ana", 0, 1).elementos().get(0);
        assertThat(leida.leidaEn()).isEqualTo(Instant.EPOCH.plusSeconds(90));
        assertThat(leida.entrega()).isEqualTo(EstadoEntrega.ENVIADA);
    }

    @Test void restriccionUnicaDeduplicaInclusoConDosConsumidores() throws Exception {
        var original = aviso("ana", Instant.EPOCH);
        var duplicado = new Notificacion(UUID.randomUUID(), original.aviso(), original.canal(), original.entrega(), null);
        var inicio = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> { inicio.await(); return avisos.registrarSiAusente(original); });
            var b = pool.submit(() -> { inicio.await(); return avisos.registrarSiAusente(duplicado); });
            inicio.countDown();
            assertThat(List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(avisos.listar("ana", 0, 10).total()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void unIdEnConflictoNoSeConfundeConDuplicadoDeEvento() {
        var original = aviso("ana", Instant.EPOCH);
        avisos.registrarSiAusente(original);
        var conflicto = new Notificacion(original.id(), aviso("otro", Instant.EPOCH).aviso(),
                CanalPreferido.PUSH, EstadoEntrega.PENDIENTE, null);
        assertThatThrownBy(() -> avisos.registrarSiAusente(conflicto)).isInstanceOf(jakarta.persistence.PersistenceException.class);
        assertThat(avisos.listar("otro", 0, 10).total()).isZero();
        assertThat(avisos.listar("ana", 0, 10).total()).isEqualTo(1);
    }
}
