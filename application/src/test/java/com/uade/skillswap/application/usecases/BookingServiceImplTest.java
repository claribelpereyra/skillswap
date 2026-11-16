package com.uade.skillswap.application.usecases;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.BookingHistoryEntry;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.application.dto.RequestBookingCommand;
import com.uade.skillswap.application.ports.out.BookingPricingPolicy;
import com.uade.skillswap.application.ports.out.BookablePublication;
import com.uade.skillswap.application.ports.out.BookingRepository;
import com.uade.skillswap.application.ports.out.PublicationAvailability;
import com.uade.skillswap.application.ports.out.PublicationCatalog;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.WalletRepository;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.InsufficientBalanceException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.pricing.BasePrice;
import com.uade.skillswap.domain.pricing.FixedPromotion;
import com.uade.skillswap.domain.pricing.PercentagePromotion;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.domain.wallet.FullRefundStrategy;
import com.uade.skillswap.domain.wallet.RefundPolicy;
import com.uade.skillswap.domain.wallet.RefundStrategy;
import com.uade.skillswap.domain.wallet.TieredRefundStrategy;
import com.uade.skillswap.domain.wallet.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BookingServiceImplTest {

    private static final UserId CLIENT = new UserId("ana");
    private static final UserId PROVIDER = new UserId("bruno");
    private static final UserId OUTSIDER = new UserId("carla");
    private static final long PUBLICATION_ID = 10L;
    private static final long BOOKING_ID = 99L;
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final TimeSlot SLOT = new TimeSlot(1L,
            Instant.parse("2026-11-19T21:00:00Z"), Instant.parse("2026-11-19T22:00:00Z"));
    private static final TimeSlot OTHER_SLOT = new TimeSlot(2L,
            Instant.parse("2026-11-20T13:00:00Z"), Instant.parse("2026-11-20T14:00:00Z"));
    private static final BookablePublication PUBLICATION = new BookablePublication(
            PUBLICATION_ID, PROVIDER, true, new SkillCoins(100), List.of(SLOT, OTHER_SLOT));

    private BookingRepository bookings;
    private PublicationCatalog publications;
    private PublicationAvailability availability;
    private WalletRepository wallets;
    private BookingPricingPolicy pricingPolicy;
    private RefundStrategy refundStrategy;
    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        bookings = mock(BookingRepository.class);
        publications = mock(PublicationCatalog.class);
        availability = mock(PublicationAvailability.class);
        wallets = mock(WalletRepository.class);
        pricingPolicy = BookingPricingPolicy.basePrice();
        refundStrategy = tieredRefundStrategy();
        UnitOfWork direct = Supplier::get;
        service = newService(direct);
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(publications.findBookable(PUBLICATION_ID)).thenReturn(Optional.of(PUBLICATION));
    }

    // Solicitar

    @Test
    void requestCreatesAPendingBookingWithThePublicationData() {

        BookingView view = service.request(new RequestBookingCommand(CLIENT, PUBLICATION_ID, 1L, "Hola", "Entrevista"));

        assertThat(view.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(view.providerId()).isEqualTo("bruno");
        assertThat(view.price()).isEqualTo(100);
        assertThat(view.slotId()).isEqualTo(1L);
        verify(bookings).save(any(Booking.class));
    }

    @Test
    void guardaElPrecioPromocionalSegunClienteYPublicacion() {
        pricingPolicy = (clientId, publication) -> clientId.equals(CLIENT)
                ? new FixedPromotion(new PercentagePromotion(new BasePrice(publication.price()), 10),
                        new SkillCoins(5)).total()
                : publication.price();
        service = newService(Supplier::get);

        BookingView view = service.request(new RequestBookingCommand(CLIENT, PUBLICATION_ID, 1L, "Hola", "Entrevista"));
        BookingView regularPrice = service.request(
                new RequestBookingCommand(OUTSIDER, PUBLICATION_ID, 1L, "Hola", "Entrevista"));

        assertThat(view.price()).isEqualTo(85);
        assertThat(regularPrice.price()).isEqualTo(100);
        verify(bookings).save(org.mockito.ArgumentMatchers.argThat(booking ->
                booking.getPrice().equals(new SkillCoins(85))));
    }

    @Test
    void requestFailsWhenThePublicationDoesNotExist() {
        when(publications.findBookable(PUBLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.request(new RequestBookingCommand(CLIENT, PUBLICATION_ID, 1L, "Hola", "Entrevista")))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Publication not found with id " + PUBLICATION_ID);
        verify(bookings, never()).save(any());
    }

    @Test
    void requestFailsWhenThePublicationIsNotPublished() {
        when(publications.findBookable(PUBLICATION_ID)).thenReturn(Optional.of(
                new BookablePublication(PUBLICATION_ID, PROVIDER, false, new SkillCoins(100), List.of(SLOT))));

        assertThatThrownBy(() -> service.request(new RequestBookingCommand(CLIENT, PUBLICATION_ID, 1L, "Hola", "Entrevista")))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("A publication that is not published does not accept requests");
        verify(bookings, never()).save(any());
    }

    @Test
    void requestFailsWhenTheSlotIsNotFromThePublication() {

        assertThatThrownBy(() -> service.request(new RequestBookingCommand(CLIENT, PUBLICATION_ID, 7L, "Hola", "Entrevista")))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The slot does not belong to the publication");
    }

    // Aceptar y reservar el cupo

    @Test
    void acceptReservesTheSpotAndSavesTheBooking() {
        stored(pending());

        BookingView view = service.accept(BOOKING_ID, PROVIDER);

        assertThat(view.status()).isEqualTo(BookingStatus.ACCEPTED);
        verify(availability).reserve(PUBLICATION_ID, 1L);
        verify(bookings).save(any(Booking.class));
    }

    @Test
    void acceptFailsWithoutSavingWhenThereIsNoCapacityLeft() {
        stored(pending());
        doThrow(new BusinessRuleException("No capacity left for this publication"))
                .when(availability).reserve(PUBLICATION_ID, 1L);

        assertThatThrownBy(() -> service.accept(BOOKING_ID, PROVIDER))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("No capacity left for this publication");
        verify(bookings, never()).save(any());
    }

    @Test
    void acceptFailsWhenTheBookingDoesNotExist() {
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(BOOKING_ID, PROVIDER))
                .isExactlyInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Booking not found with id 99");
    }

    @Test
    void pagaElPrecioPersistidoYConfirmaLaContratacion() {
        Booking booking = persisted(accepted());
        Wallet client = wallet(CLIENT, 100);
        Wallet provider = wallet(PROVIDER, 0);
        stored(booking);
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(client));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(provider));

        BookingView view = service.pay(BOOKING_ID, CLIENT);

        assertThat(view.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(client.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(provider.balance()).isEqualTo(new SkillCoins(100));
        assertThat(client.movements()).last().extracting(movement -> movement.amount())
                .isEqualTo(new SkillCoins(100));
        verify(wallets).save(client);
        verify(wallets).save(provider);
        verify(bookings).save(booking);
    }

    @Test
    void permitePagarUnaContratacionGratuitaSinMovimientos() {
        Booking freeBooking = persisted(accepted(new SkillCoins(0)));
        stored(freeBooking);

        BookingView view = service.pay(BOOKING_ID, CLIENT);

        assertThat(view.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(view.price()).isZero();
        verifyNoInteractions(wallets);
        verify(bookings).save(freeBooking);
    }

    @Test
    void saldoInsuficienteNoConfirmaNiGuardaLaContratacion() {
        Booking booking = persisted(accepted());
        Wallet client = wallet(CLIENT, 10);
        Wallet provider = wallet(PROVIDER, 0);
        stored(booking);
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(client));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> service.pay(BOOKING_ID, CLIENT))
                .isExactlyInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance: 10 < 100");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
        assertThat(client.balance()).isEqualTo(new SkillCoins(10));
        assertThat(provider.balance()).isEqualTo(SkillCoins.ZERO);
        verify(bookings, never()).save(any());
        verify(wallets, never()).save(any());
    }

    @Test
    void cancelacionReintegraYLiberaElCupoEnLaMismaUnidadDeTrabajo() {
        Booking booking = persisted(confirmed());
        Wallet client = wallet(CLIENT, 100);
        Wallet provider = Wallet.open(PROVIDER);
        client.payBookingTo(provider, booking.getPrice(), BOOKING_ID, NOW.minusSeconds(60));
        stored(booking);
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(client));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(provider));

        BookingView view = service.cancel(BOOKING_ID, CLIENT, "No puedo asistir");

        assertThat(view.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(client.balance()).isEqualTo(new SkillCoins(100));
        assertThat(provider.balance()).isEqualTo(SkillCoins.ZERO);
        verify(wallets).save(provider);
        verify(wallets).save(client);
        verify(availability).release(PUBLICATION_ID, 1L);
        verify(bookings).save(booking);
    }

    @Test
    void saldoInsuficienteDelProveedorImpideCancelarYLiberarElCupo() {
        Booking booking = persisted(confirmed());
        Wallet client = wallet(CLIENT, 100);
        Wallet provider = Wallet.open(PROVIDER);
        Wallet thirdParty = wallet(new UserId("carla"), 0);
        client.payBookingTo(provider, booking.getPrice(), BOOKING_ID, NOW.minusSeconds(60));
        provider.payBookingTo(thirdParty, booking.getPrice(), BOOKING_ID + 1, NOW.minusSeconds(30));
        stored(booking);
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(client));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> service.cancel(BOOKING_ID, CLIENT, "No puedo asistir"))
                .isExactlyInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance: 0 < 100");

        assertThat(client.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(provider.balance()).isEqualTo(SkillCoins.ZERO);
        verify(bookings, never()).save(any());
        verify(availability, never()).release(anyLong(), anyLong());
        verify(wallets, never()).save(any());
    }

    @Test
    void dosEstrategiasCalculanImportesDistintosParaLaMismaCancelacion() {
        Booking tieredBooking = persisted(confirmed(new SkillCoins(35), shortNoticeSlot()));
        Booking fullBooking = persisted(confirmed(new SkillCoins(35), shortNoticeSlot()));
        Wallet tieredClient = wallet(CLIENT, 100);
        Wallet tieredProvider = Wallet.open(PROVIDER);
        tieredClient.payBookingTo(tieredProvider, new SkillCoins(35), BOOKING_ID, NOW.minusSeconds(60));
        Wallet fullClient = wallet(CLIENT, 100);
        Wallet fullProvider = Wallet.open(PROVIDER);
        fullClient.payBookingTo(fullProvider, new SkillCoins(35), BOOKING_ID, NOW.minusSeconds(60));

        stored(tieredBooking);
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(tieredClient));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(tieredProvider));
        service.cancel(BOOKING_ID, CLIENT, "Cambio de planes");
        assertThat(tieredClient.balance()).isEqualTo(new SkillCoins(65));

        stored(fullBooking);
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(fullClient));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(fullProvider));
        refundStrategy = new FullRefundStrategy();
        service = newService(Supplier::get);
        service.cancel(BOOKING_ID, CLIENT, "Cambio de planes");

        assertThat(fullClient.balance()).isEqualTo(new SkillCoins(100));
        assertThat(fullProvider.balance()).isEqualTo(SkillCoins.ZERO);
    }

    // Rechazar, iniciar y completar

    @Test
    void rejectStartAndCompleteChangeTheStateAndSave() {
        stored(pending());
        assertThat(service.reject(BOOKING_ID, PROVIDER, "Sin disponibilidad").status()).isEqualTo(BookingStatus.REJECTED);

        stored(confirmed());
        assertThat(service.start(BOOKING_ID, PROVIDER).status()).isEqualTo(BookingStatus.IN_PROGRESS);

        assertThat(service.complete(BOOKING_ID, CLIENT).status()).isEqualTo(BookingStatus.COMPLETED);
    }

    // Reprogramación

    @Test
    void proposeRescheduleUsesASlotOfTheSamePublication() {
        stored(accepted());

        BookingView view = service.proposeReschedule(BOOKING_ID, PROVIDER, 2L);

        assertThat(view.proposedSlotId()).isEqualTo(2L);
        assertThat(view.rescheduleProposedBy()).isEqualTo("bruno");
        assertThatThrownBy(() -> service.proposeReschedule(BOOKING_ID, PROVIDER, 7L))
                .isExactlyInstanceOf(BusinessRuleException.class)
                .hasMessage("The slot does not belong to the publication");
    }

    @Test
    void acceptRescheduleReleasesThePreviousSlotBeforeReservingTheNewOne() {
        Booking booking = accepted();
        booking.proposeReschedule(PROVIDER, OTHER_SLOT);
        stored(booking);

        BookingView view = service.acceptReschedule(BOOKING_ID, CLIENT);

        assertThat(view.slotId()).isEqualTo(2L);
        InOrder order = inOrder(availability);
        order.verify(availability).release(PUBLICATION_ID, 1L);
        order.verify(availability).reserve(PUBLICATION_ID, 2L);
    }

    @Test
    void rejectRescheduleKeepsTheSlotWithoutTouchingAvailability() {
        Booking booking = accepted();
        booking.proposeReschedule(PROVIDER, OTHER_SLOT);
        stored(booking);

        BookingView view = service.rejectReschedule(BOOKING_ID, CLIENT);

        assertThat(view.slotId()).isEqualTo(1L);
        assertThat(view.proposedSlotId()).isNull();
        verifyNoInteractions(availability);
    }

    // Consultas

    @Test
    void onlyParticipantsCanSeeTheHistory() {
        stored(accepted());

        List<BookingHistoryEntry> history = service.history(BOOKING_ID, CLIENT);

        assertThat(history).extracting(BookingHistoryEntry::to)
                .containsExactly(BookingStatus.PENDING, BookingStatus.ACCEPTED);
        assertThatThrownBy(() -> service.history(BOOKING_ID, OUTSIDER))
                .isExactlyInstanceOf(AccessDeniedException.class)
                .hasMessage("User carla is not allowed to view the history of booking " + BOOKING_ID);
    }

    @Test
    void listsTheBookingsOfTheLoggedUserPaginated() {
        PageRequest page = PageRequest.first();
        when(bookings.findByClient(CLIENT, page)).thenReturn(new Page<>(List.of(pending()), page, 1));
        when(bookings.findByProvider(CLIENT, page)).thenReturn(Page.empty(page));

        assertThat(service.listAsClient(CLIENT, page).items()).singleElement()
                .extracting(BookingView::status).isEqualTo(BookingStatus.PENDING);
        assertThat(service.listAsProvider(CLIENT, page).items()).isEmpty();
    }

    private void stored(Booking booking) {
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
    }

    private BookingServiceImpl newService(UnitOfWork unitOfWork) {
        return new BookingServiceImpl(bookings, publications, availability, wallets, pricingPolicy, refundStrategy,
                unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static RefundStrategy tieredRefundStrategy() {
        return new TieredRefundStrategy(new RefundPolicy(Duration.ofDays(2), Duration.ofDays(1)));
    }

    private static Wallet wallet(UserId owner, long initialBalance) {
        Wallet wallet = Wallet.open(owner);
        if (initialBalance > 0) {
            wallet.topUp(new SkillCoins(initialBalance), NOW.minusSeconds(600));
        }
        return wallet;
    }

    private static Booking persisted(Booking booking) {
        return Booking.rebuild(BOOKING_ID, booking.getPublicationId(), booking.getClientId(), booking.getProviderId(),
                booking.getSlot(), booking.getMessage(), booking.getGoal(), booking.getPrice(),
                booking.getHistory(), booking.getPendingReschedule().orElse(null));
    }

    private static Booking pending() {
        return Booking.request(PUBLICATION_ID, CLIENT, PROVIDER, SLOT, "Hola", "Entrevista", new SkillCoins(100));
    }

    private static Booking accepted() {
        return accepted(new SkillCoins(100));
    }

    private static Booking accepted(SkillCoins price) {
        Booking booking = Booking.request(PUBLICATION_ID, CLIENT, PROVIDER, SLOT, "Hola", "Entrevista", price);
        booking.accept(PROVIDER);
        return booking;
    }

    private static Booking confirmed() {
        return confirmed(new SkillCoins(100), SLOT);
    }

    private static Booking confirmed(SkillCoins price, TimeSlot slot) {
        Booking booking = Booking.request(PUBLICATION_ID, CLIENT, PROVIDER, slot, "Hola", "Entrevista", price);
        booking.accept(PROVIDER);
        booking.confirmPayment(CLIENT);
        return booking;
    }

    private static TimeSlot shortNoticeSlot() {
        return new TimeSlot(1L, NOW.plus(Duration.ofHours(12)), NOW.plus(Duration.ofHours(13)));
    }

}
