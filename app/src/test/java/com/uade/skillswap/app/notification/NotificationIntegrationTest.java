package com.uade.skillswap.app.notification;

import com.uade.skillswap.application.eventos.*;
import com.uade.skillswap.application.usecases.*;
import com.uade.skillswap.application.notificaciones.FabricaCanalNotificacion;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.notification.NotificationControllerImpl;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.notificaciones.*;
import com.uade.skillswap.infrastructure.adapters.notificaciones.*;
import com.uade.skillswap.infrastructure.persistence.PersistenceConfiguration;
import com.uade.skillswap.infrastructure.persistence.notificaciones.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class NotificationIntegrationTest {
    @Test void case8PersistsTwoChannelsAndExposesOnlyTheActiveUsersInbox() {
        var persistence = new PersistenceConfiguration("jdbc:h2:mem:notifications-" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        persistence.migrate();
        try (var factory = persistence.createEntityManagerFactory()) {
            var repository = new RepositorioNotificacionesJpa(factory);
            var preferences = new PreferenciaNotificacionCasoDeUso(new RepositorioPreferenciasJpa(factory));
            preferences.cambiar("client", CanalPreferido.EMAIL);
            preferences.cambiar("provider", CanalPreferido.PUSH);
            var email = new EmailMock();
            var push = new PushMock();
            var internal = new InternaMock();
            var processor = new ProcesarEventoNotificableCasoDeUso(preferences, repository,
                    new FabricaCanalNotificacion(List.of(email, push, internal)));
            var publisher = new PublicadorEventosEnMemoria();
            publisher.suscribir(new SuscriptorNotificaciones(processor));
            var now = Instant.parse("2026-11-15T12:00:00Z");
            var event = new EventoNotificable(UUID.randomUUID(), now, TipoEventoNotificable.PAGO_CONFIRMADO,
                    "booking-1", List.of("client", "provider"), "Pago confirmado: 100 SkillCoins");
            publisher.publicar(event);
            publisher.publicar(event);
            assertThat(email.enviados()).hasSize(1);
            assertThat(push.enviados()).hasSize(1);
            assertThat(internal.enviados()).isEmpty();
            var active = new AtomicReference<>(new UserId("client"));
            var controller = new NotificationControllerImpl(new BandejaNotificacionCasoDeUso(repository,
                    Clock.fixed(now.plusSeconds(60), ZoneOffset.UTC)), active::get);
            var clientPage = controller.listar(0, 1);
            assertThat(clientPage.isSuccess()).isTrue();
            assertThat(clientPage.getData().totalItems()).isEqualTo(1);
            var clientNotice = clientPage.getData().items().get(0);
            assertThat(clientNotice.channel()).isEqualTo("EMAIL");
            assertThat(clientNotice.delivery()).isEqualTo("ENVIADA");
            active.set(new UserId("provider"));
            assertThat(controller.listar(0, 1).getData().items().get(0).channel()).isEqualTo("PUSH");
            assertThat(controller.marcarLeida(clientNotice.id()).getErrorType()).isEqualTo(ControllerResult.ErrorType.NOT_FOUND);
            active.set(new UserId("client"));
            assertThat(controller.marcarLeida(clientNotice.id()).isSuccess()).isTrue();
            assertThat(controller.marcarLeida(clientNotice.id()).isSuccess()).isTrue();
            var read = controller.listar(0, 1).getData().items().get(0);
            assertThat(read.read()).isTrue();
            assertThat(read.readAt()).isEqualTo(now.plusSeconds(60));
            active.set(null);
            assertThat(controller.listar(0, 1).getErrorType()).isEqualTo(ControllerResult.ErrorType.ACCESS_DENIED);
        }
    }
}
