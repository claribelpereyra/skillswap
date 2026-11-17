package com.uade.skillswap.app;

import ar.edu.uade.skillswap.model.Models.*;
import com.uade.skillswap.app.presentation.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class RealUiControllersIntegrationTest {
    private final TestDatabase database = new TestDatabase();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"),ZoneOffset.UTC);
    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    @AfterEach void dropDatabase() { database.close(); }
    private ApplicationRuntime open() { return CompositionRoot.open(database.configuration(),CLOCK,Map.of("SKILLSWAP_DEMO","true")); }
    private RealUiControllers user(ApplicationRuntime runtime,String user) {
        assertThat(runtime.controllers().session().signIn(user).isSuccess()).isTrue();
        return new RealUiControllers(runtime.controllers(),user,ZONE);
    }
    private static FiltrosBusqueda filters() { return new FiltrosBusqueda("","Todas",null,0,"Relevancia"); }

    @Test void kitContractUsesRealPaymentsRatingsAndNotificationsAcrossUserSwitches() {
        try(var runtime=open()) {
            var mateo=user(runtime,"user-mateo");
            var offers=mateo.buscar(filters());
            assertThat(offers).hasSize(2).allSatisfy(p->{assertThat(p.descripcion()).isNotBlank();assertThat(p.duracionMinutos()).isEqualTo(60);});
            var publication=offers.stream().filter(p->p.proveedor().equals("Sofía Méndez")).findFirst().orElseThrow();
            var slots=mateo.franjas("user-mateo",publication.id());
            var booking=mateo.solicitar("user-mateo",publication.id(),slots.get(0).id(),"Hola Sofía","Practicar Java");
            assertThat(booking.estado()).isEqualTo(EstadoContratacion.PENDIENTE);
            assertThat(booking.titulo()).isEqualTo(publication.titulo());
            assertThat(booking.contraparte()).isEqualTo("Sofía Méndez");
            assertThatThrownBy(()->mateo.avanzar("user-mateo",booking.id())).isInstanceOf(UiOperationException.class)
                    .hasMessage("Only the provider can accept a booking");
            var sofia=user(runtime,"user-sofia");
            sofia.avanzar("user-sofia",booking.id());
            user(runtime,"user-mateo");
            // Reutiliza la instancia previa: debe releer el estado aceptado por Sofía.
            assertThat(mateo.avanzar("user-mateo",booking.id()).estado()).isEqualTo(EstadoContratacion.CONFIRMADA);
            assertThat(mateo.consultarSaldo("user-mateo")).isEqualTo(400);
            assertThat(mateo.listarMovimientos("user-mateo")).anySatisfy(m->{assertThat(m.importe()).isEqualTo(-100);assertThat(m.saldo()).isEqualTo(400);});
            user(runtime,"user-sofia");
            sofia.reprogramar("user-sofia",booking.id(),slots.get(1).id());
            user(runtime,"user-mateo");
            assertThat(mateo.tieneReprogramacion("user-mateo",booking.id())).isTrue();
            mateo.responderReprogramacion("user-mateo",booking.id(),true);
            user(runtime,"user-sofia");sofia.avanzar("user-sofia",booking.id());
            user(runtime,"user-mateo");mateo.avanzar("user-mateo",booking.id());
            mateo.calificar("user-mateo",booking.id(),5,"Excelente");
            assertThat(mateo.comprobante("user-mateo",booking.id())).contains("100 SkillCoins");
            assertThatThrownBy(()->mateo.calificar("user-mateo",booking.id(),5,"Repetida")).isInstanceOf(UiOperationException.class);
            assertThat(mateo.listarNotificaciones("user-mateo")).hasSize(5);
            mateo.marcarTodasLeidas("user-mateo");
            assertThat(mateo.listarNotificaciones("user-mateo")).allSatisfy(n->assertThat(n.leida()).isTrue());
            mateo.cambiarCanal("user-mateo","INTERNA");assertThat(mateo.canal("user-mateo")).isEqualTo("INTERNA");
            user(runtime,"user-sofia");
            assertThat(sofia.verPerfil("user-sofia").reputacion()).isEqualTo(5);
            assertThat(sofia.consultarSaldo("user-sofia")).isEqualTo(600);
            sofia.cambiarEstado("user-sofia",publication.id(),EstadoPublicacion.CERRADA);
            assertThat(sofia.listarContrataciones("user-sofia")).anySatisfy(c->assertThat(c.titulo()).isEqualTo(publication.titulo()));
            assertThatThrownBy(()->mateo.consultarSaldo("user-mateo")).isInstanceOf(UiOperationException.class).hasMessageContaining("sesión cambió");
        }
    }

    @Test void realDraftsPaginationAndDemoRestartDoNotDuplicateOrResetData() {
        try(var runtime=open()) {
            var mateo=user(runtime,"user-mateo");
            mateo.cambiarCanal("user-mateo","INTERNA");
            var category=runtime.controllers().catalog().list(0,100).getData().items().stream().filter(c->c.kind().equals("CATEGORY")).findFirst().orElseThrow();
            var skill=runtime.controllers().catalog().list(0,100).getData().items().stream().filter(c->c.kind().equals("SKILL")).findFirst().orElseThrow();
            DemoScenario.prepare(runtime.controllers(),CLOCK);user(runtime,"user-mateo");
            assertThat(runtime.controllers().catalog().list(0,100).getData().totalItems()).isEqualTo(2);
            assertThat(mateo.consultarSaldo("user-mateo")).isEqualTo(500);
            assertThat(mateo.canal("user-mateo")).isEqualTo("INTERNA");
            for(int i=0;i<13;i++) {
                var start=LocalDateTime.of(2026,12,1,10,0).plusDays(i);
                var draft=mateo.crearBorrador("user-mateo","Oferta "+i,"Descripción persistida",category.name(),Modalidad.PRESENCIAL,
                        90,50,"SERVICE",List.of(new Franja(null,start,start.plusMinutes(90),false)));
                mateo.cambiarEstado("user-mateo",draft.id(),EstadoPublicacion.PUBLICADA);
            }
            var first=mateo.buscar(filters());assertThat(first).hasSize(12);assertThat(mateo.haySiguiente("buscar")).isTrue();
            mateo.cambiarPagina("buscar",1);var second=mateo.buscar(filters());assertThat(second).hasSize(3);
            assertThat(second.stream().map(Publicacion::id)).doesNotContainAnyElementsOf(first.stream().map(Publicacion::id).toList());
            assertThat(mateo.haySiguiente("buscar")).isFalse();
            assertThatThrownBy(()->mateo.buscar(new FiltrosBusqueda("","Todas",Modalidad.HIBRIDA,0,"Relevancia")))
                    .isInstanceOf(UiOperationException.class).hasMessageContaining("híbrida");
        }
    }
}
