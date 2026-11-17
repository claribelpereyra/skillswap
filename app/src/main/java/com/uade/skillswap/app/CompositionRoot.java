package com.uade.skillswap.app;

import com.uade.skillswap.app.presentation.ApplicationControllers;
import com.uade.skillswap.application.eventos.*;
import com.uade.skillswap.application.notificaciones.FabricaCanalNotificacion;
import com.uade.skillswap.application.usecases.*;
import com.uade.skillswap.application.usecases.profile.*;
import com.uade.skillswap.application.usecases.publication.*;
import com.uade.skillswap.application.usecases.rating.*;
import com.uade.skillswap.application.usecases.wallet.*;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.booking.BookingControllerImpl;
import com.uade.skillswap.controllers.booking.BookingReceiptControllerImpl;
import com.uade.skillswap.application.usecases.booking.GetBookingReceiptService;
import com.uade.skillswap.application.usecases.booking.PromotionalPricingPolicy;
import com.uade.skillswap.controllers.notification.*;
import com.uade.skillswap.controllers.profile.ProfileControllerImpl;
import com.uade.skillswap.controllers.publication.*;
import com.uade.skillswap.controllers.rating.RatingControllerImpl;
import com.uade.skillswap.controllers.session.SessionController;
import com.uade.skillswap.controllers.wallet.*;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.pricing.FixedPromotion;
import com.uade.skillswap.domain.pricing.PercentagePromotion;
import com.uade.skillswap.domain.pricing.Price;
import com.uade.skillswap.domain.wallet.*;
import com.uade.skillswap.infrastructure.adapters.*;
import com.uade.skillswap.infrastructure.adapters.notificaciones.*;
import com.uade.skillswap.infrastructure.configuracion.ConfiguracionFuncional;
import com.uade.skillswap.infrastructure.persistence.*;
import com.uade.skillswap.infrastructure.persistence.booking.JpaBookingRepository;
import com.uade.skillswap.infrastructure.persistence.booking.JpaBookingLabelsQuery;
import com.uade.skillswap.infrastructure.persistence.catalog.JpaCategoryTree;
import com.uade.skillswap.infrastructure.persistence.catalog.JpaCatalogQuery;
import com.uade.skillswap.infrastructure.persistence.notificaciones.*;
import com.uade.skillswap.infrastructure.persistence.profile.*;
import com.uade.skillswap.infrastructure.persistence.publication.*;
import com.uade.skillswap.infrastructure.persistence.rating.*;
import com.uade.skillswap.infrastructure.persistence.wallet.JpaWalletRepository;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/** Único punto de composición de implementaciones concretas en producción. */
public final class CompositionRoot {
    private CompositionRoot() { }

    public static ApplicationRuntime open(PersistenceConfiguration database, Clock clock,
                                           Map<String, String> environment) {
        Objects.requireNonNull(clock);
        var configuration = ConfiguracionFuncional.desde(environment);
        database.migrate();
        var factory = database.createEntityManagerFactory();
        try {
            EntityManagerContext.init(factory);
            var identity = new MockIdentityAdapter();
            var work = new JpaUnitOfWork();
            var profiles = new JpaProfileRepository();
            var publications = new JpaPublicationRepository();
            var bookings = new JpaBookingRepository();
            var ratings = new JpaRatingRepository();
            var wallets = new JpaWalletRepository();
            var notificationRepository = new RepositorioNotificacionesJpa(factory);
            var preferences = new PreferenciaNotificacionCasoDeUso(new RepositorioPreferenciasJpa(factory));
            var channels = new FabricaCanalNotificacion(List.of(new EmailMock(), new PushMock(), new InternaMock()));
            var processor = new ProcesarEventoNotificableCasoDeUso(preferences, notificationRepository, channels);
            var refund = new TieredRefundStrategy(new RefundPolicy(configuration.anticipacionReintegroTotal(),
                    configuration.anticipacionReintegroParcial()));
            var bookingService = new BookingServiceImpl(bookings, new PublicationCatalogAdapter(publications),
                    new PublicationAvailabilityAdapter(publications), wallets, new PromotionalPricingPolicy(promociones(configuration)),
                    refund, work, clock, new JpaBookingLabelsQuery());
            bookingService.addObserver(new BookingNotificationObserver(processor));
            var catalog = new JpaCategoryTree();
            var search = new SearchPublicationsService(new JpaPublicationSearchQuery(), catalog);
            var metadata = new JpaRecommendationMetadataQuery();
            var affinity = new SkillAffinityRecommendationStrategy(metadata);
            var popularity = new RecentPopularityRecommendationStrategy(metadata, clock, Duration.ofDays(30));
            var selected = configuration.estrategiaRecomendacion()
                    == ConfiguracionFuncional.EstrategiaRecomendacion.AFINIDAD ? affinity : popularity;
            var session = new SessionController() {
                @Override public ControllerResult<String> signIn(String userId) {
                    return ControllerResult.of(() -> {
                        var user = new UserId(userId);
                        identity.signIn(user);
                        return user.value();
                    });
                }
                @Override public void signOut() { identity.signOut(); }
                @Override public String currentUserId() {
                    var user = identity.currentUserId();
                    return user == null ? null : user.value();
                }
            };
            var controllers = new ApplicationControllers(session, new CatalogControllerImpl(new ListCatalogService(new JpaCatalogQuery()),
                            new ManageCatalogService(catalog, work), identity),
                    new ProfileControllerImpl(new CreateProfileService(profiles, work),
                            new UpdateProfileService(profiles, work), new ManageSkillsService(profiles, work),
                            new DeactivateProfileService(profiles, work),
                            new GetPublicProfileFacade(profiles, new JpaActivePublicationsQuery(publications), ratings), identity),
                    new PublicationControllerImpl(new CreateDraftService(publications, new JpaAuthorProfileQuery(), catalog, work, clock),
                            new EditDraftService(publications, catalog, work), new ChangePublicationStatusService(publications, work, clock),
                            new GetPublicationService(publications), new ListMyPublicationsService(publications), identity),
                    new SearchControllerImpl(search),
                    new RecommendationControllerImpl(new RecommendPublicationsService(search),
                            Map.of("SKILL_AFFINITY", affinity, "RECENT_POPULARITY", popularity), selected, identity),
                    new BookingControllerImpl(bookingService, identity),
                    new BookingReceiptControllerImpl(new GetBookingReceiptService(bookings, wallets), identity),
                    new WalletControllerImpl(new LoadWalletService(wallets, new SimulatedSkillCoinGateway(), work, clock), identity),
                    new WalletQueryControllerImpl(new GetWalletService(wallets), identity),
                    new RatingControllerImpl(new RegisterRatingService(ratings, profiles,
                            new JpaEligibleBookingQuery(bookings), work), new ListRatingsService(ratings, profiles), identity),
                    new NotificationControllerImpl(new BandejaNotificacionCasoDeUso(notificationRepository, clock), identity),
                    new NotificationPreferencesControllerImpl(preferences, identity));
            if (Boolean.parseBoolean(environment.getOrDefault("SKILLSWAP_DEMO", "false"))) {
                DemoScenario.prepare(controllers, clock);
            }
            return new ApplicationRuntime(controllers, factory);
        } catch (RuntimeException | Error error) {
            factory.close();
            throw error;
        }
    }

    private static Map<UserId, List<UnaryOperator<Price>>> promociones(ConfiguracionFuncional configuration) {
        Map<UserId, List<UnaryOperator<Price>>> porProveedor = new HashMap<>();
        configuration.promocionesPorProveedor().forEach((proveedor, promociones) -> porProveedor.put(
                new UserId(proveedor), promociones.stream().map(CompositionRoot::decorador).toList()));
        return porProveedor;
    }

    private static UnaryOperator<Price> decorador(ConfiguracionFuncional.PromocionConfigurada promocion) {
        return switch (promocion.tipo()) {
            case PORCENTAJE -> price -> new PercentagePromotion(price, (int) promocion.valor());
            case FIJO -> price -> new FixedPromotion(price, new SkillCoins(promocion.valor()));
        };
    }
}
