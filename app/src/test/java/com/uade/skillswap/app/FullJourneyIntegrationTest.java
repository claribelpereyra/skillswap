package com.uade.skillswap.app;

import com.uade.skillswap.app.presentation.ApplicationControllers;
import com.uade.skillswap.application.dto.publication.PublicationView;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.profile.CreateProfileForm;
import com.uade.skillswap.controllers.publication.PublicationForm;
import com.uade.skillswap.controllers.publication.SearchForm;
import com.uade.skillswap.controllers.publication.RecommendationForm;
import com.uade.skillswap.controllers.rating.RatingForm;
import com.uade.skillswap.infrastructure.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class FullJourneyIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-11-16T10:00:00Z");
    private final TestDatabase database = new TestDatabase();

    @AfterEach void dropTestDatabase() { database.close(); }

    private ApplicationRuntime open(Map<String, String> environment) {
        var runtime = CompositionRoot.open(database.configuration(),
                Clock.fixed(NOW, ZoneOffset.UTC), environment);
        new JpaUnitOfWork().execute(() -> EntityManagerContext.withEntityManager(em ->
                em.createNativeQuery("INSERT INTO category(id,parent_id,name,kind) VALUES(1,NULL,'Tech','CATEGORY')")
                        .executeUpdate()));
        var app = runtime.controllers();
        for (String user : List.of("client", "provider")) {
            ok(app.session().signIn(user));
            ok(app.profiles().createProfile(user, new CreateProfileForm(user, "Java", "UTC", "PROFESSIONAL")));
            ok(app.wallet().loadCredits(user, 200L));
            ok(app.notificationPreferences().changeChannel(user.equals("client") ? "EMAIL" : "PUSH"));
        }
        return runtime;
    }

    private PublicationView publish(ApplicationControllers app) {
        ok(app.session().signIn("provider"));
        var slots = List.of(new PublicationForm.SlotForm(NOW.plus(Duration.ofDays(5)), NOW.plus(Duration.ofDays(5)).plusSeconds(3600)),
                new PublicationForm.SlotForm(NOW.plus(Duration.ofDays(6)), NOW.plus(Duration.ofDays(6)).plusSeconds(3600)));
        var draft = ok(app.publications().createDraft("provider", new PublicationForm("MENTORING", "Java mentoring",
                "Prepare interviews", 1L, "REMOTE", 60, 1, 100L, Set.of("java"), slots)));
        return ok(app.publications().changeStatus("provider", draft.id(), "PUBLISH", null));
    }

    @Test
    void publishesBooksPaysReschedulesCompletesAndRatesUsingTheProductionComposition() {
        try (var runtime = open(Map.of("SKILLSWAP_RECOMENDACION", "POPULARIDAD"))) {
            var app = runtime.controllers();
            var publication = publish(app);
            ok(app.session().signIn("client"));
            assertThat(ok(app.search().search(new SearchForm(null,null,null,null,null,null,null,null,0,10))).totalItems())
                    .isEqualTo(1);
            assertThat(ok(app.recommendations().recommend("client", new RecommendationForm(null,
                    new SearchForm(null,null,null,null,null,null,null,null,0,10)))).totalItems()).isEqualTo(1);
            long booking = ok(app.bookings().request("client", publication.id(), publication.slots().get(0).id(),
                    "Hello", "Learn Java")).id();
            ok(app.session().signIn("provider"));
            ok(app.bookings().accept("provider", booking));
            ok(app.bookings().proposeReschedule("provider", booking, publication.slots().get(1).id()));
            ok(app.session().signIn("client"));
            ok(app.bookings().acceptReschedule("client", booking));
            ok(app.bookings().pay("client", booking));
            assertThat(ok(app.walletQuery().getMyWallet()).balance()).isEqualTo(100);
            assertThat(ok(app.walletQuery().getMyWallet(0, 1)).movements().items()).hasSize(1);
            assertThat(ok(app.walletQuery().getMyWallet(0, 1)).movements().totalItems()).isEqualTo(2);
            assertThat(ok(app.walletQuery().getMyWallet(100, 1)).movements().items()).isEmpty();
            assertThat(app.walletQuery().getMyWallet(-1, 1).getErrorType()).isEqualTo(ControllerResult.ErrorType.INVALID_INPUT);
            assertThat(app.bookings().pay("client", booking).isSuccess()).isFalse();
            ok(app.session().signIn("provider"));
            ok(app.bookings().start("provider", booking));
            ok(app.session().signIn("client"));
            ok(app.bookings().complete("client", booking));
            ok(app.receipts().getReceipt(booking));
            ok(app.ratings().registerRating("client", new RatingForm(booking, "provider", 5, "Excellent")));
            assertThat(app.ratings().registerRating("client", new RatingForm(booking, "provider", 1, "Again")).isSuccess()).isFalse();
            assertThat(ok(app.profiles().getPublicProfile("provider", 0, 10)).reputation()).isEqualTo(5);
            var inbox = ok(app.notifications().listar(0, 10));
            assertThat(inbox.totalItems()).isEqualTo(5);
            assertThat(inbox.items()).extracting(n -> n.type()).containsExactlyInAnyOrder(
                    "SOLICITUD_RECIBIDA", "ACEPTACION", "REPROGRAMACION", "PAGO_CONFIRMADO", "SESION_COMPLETADA");
            assertThat(inbox.items()).allSatisfy(n -> {
                assertThat(n.channel()).isEqualTo("EMAIL");
                assertThat(n.delivery()).isEqualTo("ENVIADA");
            });
            var id = inbox.items().get(0).id();
            ok(app.notifications().marcarLeida(id));
            ok(app.notifications().marcarLeida(id));
            ok(app.session().signIn("provider"));
            assertThat(ok(app.notifications().listar(0, 10)).items()).allSatisfy(n -> assertThat(n.channel()).isEqualTo("PUSH"));
            assertThat(app.notifications().marcarLeida(id).getErrorType()).isEqualTo(ControllerResult.ErrorType.NOT_FOUND);
            assertThat(ok(app.walletQuery().getMyWallet()).balance()).isEqualTo(300);
            assertThat(ok(app.notificationPreferences().getChannel())).isEqualTo("PUSH");
            assertThat(app.notificationPreferences().changeChannel("SMS").getErrorType())
                    .isEqualTo(ControllerResult.ErrorType.INVALID_INPUT);
            ok(app.session().signIn("outsider"));
            assertThat(app.receipts().getReceipt(booking).getErrorType()).isEqualTo(ControllerResult.ErrorType.ACCESS_DENIED);
            app.session().signOut();
            assertThat(app.notificationPreferences().changeChannel("EMAIL").getErrorType())
                    .isEqualTo(ControllerResult.ErrorType.ACCESS_DENIED);
            assertThat(app.walletQuery().getMyWallet().getErrorType()).isEqualTo(ControllerResult.ErrorType.ACCESS_DENIED);
            assertThat(app.receipts().getReceipt(booking).getErrorType()).isEqualTo(ControllerResult.ErrorType.ACCESS_DENIED);
            assertThat(app.session().signIn(" ").getErrorType()).isEqualTo(ControllerResult.ErrorType.INVALID_INPUT);
            assertThat(app.session().currentUserId()).isNull();
        }
    }

    @Test
    void cancellationUsesExternalRefundThresholdsAndNotifiesBothParties() {
        try (var runtime = open(Map.of("SKILLSWAP_CANCELACION_TOTAL_HORAS", "240",
                "SKILLSWAP_CANCELACION_PARCIAL_HORAS", "24"))) {
            var app = runtime.controllers();
            var publication = publish(app);
            ok(app.session().signIn("client"));
            long booking = ok(app.bookings().request("client", publication.id(), publication.slots().get(0).id(), "Hi", "Learn")).id();
            ok(app.session().signIn("provider"));
            ok(app.bookings().accept("provider", booking));
            ok(app.session().signIn("client"));
            ok(app.bookings().pay("client", booking));
            ok(app.bookings().cancel("client", booking, "Changed plans"));
            assertThat(ok(app.walletQuery().getMyWallet()).balance()).isEqualTo(150);
            assertThat(ok(app.notifications().listar(0, 10)).items()).extracting(n -> n.type()).contains("CANCELACION");
            ok(app.session().signIn("provider"));
            assertThat(ok(app.walletQuery().getMyWallet()).balance()).isEqualTo(250);
            assertThat(ok(app.notifications().listar(0, 10)).items()).extracting(n -> n.type()).contains("CANCELACION");
            assertThat(ok(app.publications().getDetail("provider", publication.id())).availableSpots()).isEqualTo(1);
        }
    }

    // Caso de aceptación 9: promociones acumulables configuradas en el armado real, sin precio negativo.
    @Test
    void configuredPromotionsSetThePersistedPriceThatThePaymentCharges() {
        try (var runtime = open(Map.of("SKILLSWAP_PROMOCIONES", "provider=PORCENTAJE:10,FIJO:5"))) {
            var app = runtime.controllers();
            var publication = publish(app);
            ok(app.session().signIn("client"));
            var booking = ok(app.bookings().request("client", publication.id(), publication.slots().get(0).id(),
                    "Hi", "Learn"));
            assertThat(booking.price()).isEqualTo(85);
            ok(app.session().signIn("provider"));
            ok(app.bookings().accept("provider", booking.id()));
            ok(app.session().signIn("client"));
            ok(app.bookings().pay("client", booking.id()));
            assertThat(ok(app.walletQuery().getMyWallet()).balance()).isEqualTo(115);
            ok(app.session().signIn("provider"));
            assertThat(ok(app.walletQuery().getMyWallet()).balance()).isEqualTo(285);
        }
    }

    @Test
    void configuredPromotionsThatExceedThePriceLeaveItAtZero() {
        try (var runtime = open(Map.of("SKILLSWAP_PROMOCIONES", "provider=FIJO:80,PORCENTAJE:50,FIJO:100"))) {
            var app = runtime.controllers();
            var publication = publish(app);
            ok(app.session().signIn("client"));
            assertThat(ok(app.bookings().request("client", publication.id(), publication.slots().get(0).id(),
                    "Hi", "Learn")).price()).isZero();
        }
    }

    private static <T> T ok(ControllerResult<T> result) {
        assertThat(result.isSuccess()).as(result.getMessage()).isTrue();
        return result.getData();
    }

    @Test
    void anOuterRollbackDoesNotSendNotificationsOrPersistTheRequest() {
        try (var runtime = open(Map.of())) {
            var app = runtime.controllers();
            var publication = publish(app);
            ok(app.session().signIn("client"));
            assertThatThrownBy(() -> new JpaUnitOfWork().execute(() -> {
                ok(app.bookings().request("client", publication.id(), publication.slots().get(0).id(), "Hi", "Learn"));
                assertThat(ok(app.notifications().listar(0, 10)).totalItems()).isZero();
                throw new IllegalStateException("rollback outer transaction");
            })).isExactlyInstanceOf(IllegalStateException.class).hasMessage("rollback outer transaction");
            assertThat(ok(app.notifications().listar(0, 10)).totalItems()).isZero();
            assertThat(ok(app.bookings().listAsClient("client", 0, 10)).totalItems()).isZero();
            ok(app.session().signIn("provider"));
            assertThat(ok(app.notifications().listar(0, 10)).totalItems()).isZero();
        }
    }
}
