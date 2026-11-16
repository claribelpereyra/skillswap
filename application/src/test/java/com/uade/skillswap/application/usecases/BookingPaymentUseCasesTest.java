package com.uade.skillswap.application.usecases;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.out.BookingRepository;
import com.uade.skillswap.application.ports.out.PublicationAvailability;
import com.uade.skillswap.application.ports.out.PublicationCatalog;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.wallet.WalletRepository;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.InsufficientBalanceException;
import com.uade.skillswap.domain.common.InvalidTransitionException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.domain.pricing.PercentagePromotion;
import com.uade.skillswap.domain.wallet.BookingPaymentService;
import com.uade.skillswap.domain.wallet.FullRefundStrategy;
import com.uade.skillswap.domain.wallet.RefundPolicy;
import com.uade.skillswap.domain.wallet.RefundStrategy;
import com.uade.skillswap.domain.wallet.TieredRefundStrategy;
import com.uade.skillswap.domain.wallet.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingPaymentUseCasesTest {

    private static final UserId CLIENT = new UserId("ana");
    private static final UserId PROVIDER = new UserId("bruno");
    private static final UserId OUTSIDER = new UserId("carla");
    private static final long BOOKING_ID = 42L;
    private static final long PUBLICATION_ID = 10L;
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final SkillCoins PRICE = new SkillCoins(100);
    private static final TimeSlot SLOT = new TimeSlot(1L, NOW.plus(Duration.ofDays(4)),
            NOW.plus(Duration.ofDays(4)).plus(Duration.ofHours(1)));

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final PublicationCatalog publications = mock(PublicationCatalog.class);
    private final PublicationAvailability availability = mock(PublicationAvailability.class);
    private final WalletRepository wallets = mock(WalletRepository.class);
    private final BookingPaymentService payments = new BookingPaymentService(new FullRefundStrategy());
    private final UnitOfWork unitOfWork = Supplier::get;
    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(bookings, publications, availability, unitOfWork, wallets, payments,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(bookings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void payingAnAcceptedBookingTransfersFundsAndConfirmsIt() {
        Booking booking = acceptedBooking();
        Wallet clientWallet = wallet(CLIENT, 100, NOW.minusSeconds(120));
        Wallet providerWallet = Wallet.open(PROVIDER);
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(clientWallet));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(providerWallet));

        var result = service.pay(BOOKING_ID, CLIENT);

        assertThat(result.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(clientWallet.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(providerWallet.balance()).isEqualTo(PRICE);
        verify(bookings).save(booking);
        verify(wallets).save(clientWallet);
        verify(wallets).save(providerWallet);
    }

    @Test
    void payingAppliesPromotionsAndCancellationRefundsTheDiscountedAmount() {
        Booking booking = acceptedBooking();
        Wallet clientWallet = wallet(CLIENT, 100, NOW.minusSeconds(120));
        Wallet providerWallet = Wallet.open(PROVIDER);
        BookingPaymentService discountedPayments = new BookingPaymentService(
                new FullRefundStrategy(),
                price -> new PercentagePromotion(price, 10));
        BookingServiceImpl discountedService = new BookingServiceImpl(bookings, publications, availability, unitOfWork,
                wallets, discountedPayments, Clock.fixed(NOW, ZoneOffset.UTC));
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(clientWallet));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(providerWallet));

        var paid = discountedService.pay(BOOKING_ID, CLIENT);

        assertThat(paid.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(clientWallet.balance()).isEqualTo(new SkillCoins(10));
        assertThat(providerWallet.balance()).isEqualTo(new SkillCoins(90));
        assertThat(providerWallet.paymentAmountFor(BOOKING_ID)).isEqualTo(new SkillCoins(90));

        var cancelled = discountedService.cancel(BOOKING_ID, CLIENT, "No puedo asistir");

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(clientWallet.balance()).isEqualTo(new SkillCoins(100));
        assertThat(providerWallet.balance()).isEqualTo(SkillCoins.ZERO);
    }

    @Test
    void saldoInsuficienteNoPersisteSaldoNiEstadoDeContratacion() {
        Booking booking = acceptedBooking();
        Wallet clientWallet = wallet(CLIENT, 10, NOW.minusSeconds(120));
        Wallet providerWallet = Wallet.open(PROVIDER);
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(clientWallet));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(providerWallet));

        assertThatThrownBy(() -> service.pay(BOOKING_ID, CLIENT))
                .isExactlyInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance: 10 < 100");

        assertThat(clientWallet.balance()).isEqualTo(new SkillCoins(10));
        assertThat(providerWallet.balance()).isEqualTo(SkillCoins.ZERO);
        verify(bookings, never()).save(any());
        verify(wallets, never()).save(any());
    }

    @Test
    void unPagoDuplicadoNoVuelveAfectarSaldosMovimientosNiEstado() {
        Booking booking = acceptedBooking();
        Wallet clientWallet = wallet(CLIENT, 100, NOW.minusSeconds(120));
        Wallet providerWallet = Wallet.open(PROVIDER);
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(clientWallet));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(providerWallet));

        service.pay(BOOKING_ID, CLIENT);
        int clientMovementsAfterPayment = clientWallet.movements().size();
        int providerMovementsAfterPayment = providerWallet.movements().size();

        assertThatThrownBy(() -> service.pay(BOOKING_ID, CLIENT))
                .isExactlyInstanceOf(InvalidTransitionException.class)
                .hasMessage("Cannot pay in state CONFIRMED");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(clientWallet.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(providerWallet.balance()).isEqualTo(PRICE);
        assertThat(clientWallet.movements()).hasSize(clientMovementsAfterPayment);
        assertThat(providerWallet.movements()).hasSize(providerMovementsAfterPayment);
        verify(bookings, times(1)).save(booking);
        verify(wallets, times(1)).save(clientWallet);
        verify(wallets, times(1)).save(providerWallet);
    }

    @Test
    void dosPoliticasDeReintegroProducenResultadosDistintosAlCancelar() {
        CancellationOutcome partialRefund = cancelWithRefundStrategy(new TieredRefundStrategy(
                new RefundPolicy(Duration.ofDays(7), Duration.ofDays(2))));
        CancellationOutcome fullRefund = cancelWithRefundStrategy(new FullRefundStrategy());

        assertThat(partialRefund.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(partialRefund.clientBalance()).isEqualTo(new SkillCoins(50));
        assertThat(partialRefund.providerBalance()).isEqualTo(new SkillCoins(50));
        assertThat(fullRefund.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(fullRefund.clientBalance()).isEqualTo(new SkillCoins(100));
        assertThat(fullRefund.providerBalance()).isEqualTo(SkillCoins.ZERO);
    }

    @Test
    void anUnrelatedUserCannotPayTheBooking() {
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(acceptedBooking()));

        assertThatThrownBy(() -> service.pay(BOOKING_ID, OUTSIDER))
                .hasMessageContaining("Only the client can pay");

        verify(wallets, never()).findByOwnerId(any());
    }

    @Test
    void cancellingAnAcceptedBookingReleasesItsSlotWithoutTouchingWallets() {
        Booking booking = acceptedBooking();
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        var result = service.cancel(BOOKING_ID, CLIENT, "Cambio de planes");

        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
        verify(availability).release(PUBLICATION_ID, SLOT.getId());
        verify(bookings).save(booking);
        verify(wallets, never()).findByOwnerId(any());
        verify(wallets, never()).save(any());
    }

    @Test
    void cancellingAConfirmedBookingRefundsAndReleasesItsSlot() {
        Booking booking = acceptedBooking();
        booking.confirmPayment(CLIENT);
        Wallet clientWallet = wallet(CLIENT, 100, NOW.minusSeconds(120));
        Wallet providerWallet = Wallet.open(PROVIDER);
        payments.pay(BOOKING_ID, clientWallet, providerWallet, PRICE, NOW.minusSeconds(60));
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(clientWallet));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(providerWallet));

        var result = service.cancel(BOOKING_ID, CLIENT, "No puedo asistir");

        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(clientWallet.balance()).isEqualTo(PRICE);
        assertThat(providerWallet.balance()).isEqualTo(SkillCoins.ZERO);
        verify(availability).release(PUBLICATION_ID, SLOT.getId());
        verify(bookings).save(booking);
        verify(wallets).save(clientWallet);
        verify(wallets).save(providerWallet);
    }

    @Test
    void missingBookingsFailBeforeAccessingWallets() {
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pay(BOOKING_ID, CLIENT))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(wallets, never()).findByOwnerId(any());
    }

    private static Booking acceptedBooking() {
        Booking booking = Booking.request(PUBLICATION_ID, CLIENT, PROVIDER, SLOT, "Hola", "Aprender", PRICE);
        booking.accept(PROVIDER);
        return booking;
    }

    private static Wallet wallet(UserId owner, long amount, Instant toppedUpAt) {
        Wallet wallet = Wallet.open(owner);
        wallet.topUp(new SkillCoins(amount), toppedUpAt);
        return wallet;
    }

    private CancellationOutcome cancelWithRefundStrategy(RefundStrategy refundStrategy) {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        WalletRepository walletRepository = mock(WalletRepository.class);
        Booking booking = acceptedBooking();
        Wallet clientWallet = wallet(CLIENT, 100, NOW.minusSeconds(120));
        Wallet providerWallet = Wallet.open(PROVIDER);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.findByOwnerId(CLIENT)).thenReturn(Optional.of(clientWallet));
        when(walletRepository.findByOwnerId(PROVIDER)).thenReturn(Optional.of(providerWallet));
        BookingServiceImpl refundService = new BookingServiceImpl(bookingRepository, publications, availability,
                unitOfWork, walletRepository, new BookingPaymentService(refundStrategy),
                Clock.fixed(NOW, ZoneOffset.UTC));

        refundService.pay(BOOKING_ID, CLIENT);
        var result = refundService.cancel(BOOKING_ID, CLIENT, "No puedo asistir");

        return new CancellationOutcome(result.status(), clientWallet.balance(), providerWallet.balance());
    }

    private record CancellationOutcome(
            BookingStatus status,
            SkillCoins clientBalance,
            SkillCoins providerBalance) {
    }
}
