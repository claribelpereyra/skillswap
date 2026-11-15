package com.uade.skillswap.application.usecases.wallet;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.in.wallet.CancelBookingCommand;
import com.uade.skillswap.application.ports.in.wallet.GetWalletCommand;
import com.uade.skillswap.application.ports.in.wallet.PayBookingCommand;
import com.uade.skillswap.application.ports.out.TimeProvider;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.booking.BookingRepository;
import com.uade.skillswap.application.ports.out.wallet.WalletRepository;
import com.uade.skillswap.application.usecases.wallet.CancelBookingService;
import com.uade.skillswap.application.usecases.wallet.GetWalletService;
import com.uade.skillswap.application.usecases.wallet.PayBookingService;
import com.uade.skillswap.application.ports.in.wallet.WalletView;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.domain.wallet.BookingPaymentService;
import com.uade.skillswap.domain.wallet.RefundPolicy;
import com.uade.skillswap.domain.wallet.TieredRefundStrategy;
import com.uade.skillswap.domain.wallet.Wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletUseCasesTest {

    private static final UserId CLIENT = new UserId("ana");
    private static final UserId PROVIDER = new UserId("bruno");
    private static final UserId OUTSIDER = new UserId("carla");
    private static final long BOOKING_ID = 42L;
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant CREATED_AT = NOW.minusSeconds(300);
    private static final Instant PAID_AT = NOW.minusSeconds(60);
    private static final SkillCoins PRICE = new SkillCoins(100);
    private static final TimeSlot SLOT = new TimeSlot(1L, NOW.plus(Duration.ofDays(4)),
            NOW.plus(Duration.ofDays(4)).plus(Duration.ofHours(1)));

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final WalletRepository wallets = mock(WalletRepository.class);
    private final UnitOfWork unitOfWork = Supplier::get;
    private final TimeProvider timeProvider = () -> NOW;
    private final BookingPaymentService payments = new BookingPaymentService(
            new TieredRefundStrategy(new RefundPolicy(Duration.ofDays(2), Duration.ofDays(1))));

    private PayBookingService payBooking;
    private CancelBookingService cancelBooking;
    private GetWalletService getWallet;

    @BeforeEach
    void setUp() {
        payBooking = new PayBookingService(bookings, wallets, payments, unitOfWork, timeProvider);
        cancelBooking = new CancelBookingService(bookings, wallets, payments, unitOfWork, timeProvider);
        getWallet = new GetWalletService(wallets);
    }

    @Test
    void payingAnAcceptedBookingTransfersFundsAndConfirmsIt() {
        Booking booking = acceptedBooking();
        Wallet clientWallet = wallet(CLIENT, 100, NOW.minusSeconds(120));
        Wallet providerWallet = Wallet.open(PROVIDER);
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(clientWallet));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(providerWallet));

        payBooking.pay(new PayBookingCommand(CLIENT, BOOKING_ID));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(clientWallet.balance()).isEqualTo(SkillCoins.ZERO);
        assertThat(providerWallet.balance()).isEqualTo(PRICE);
        verify(bookings).save(BOOKING_ID, booking);
        verify(wallets).save(clientWallet);
        verify(wallets).save(providerWallet);
    }

    @Test
    void insufficientBalanceLeavesTheBookingUnconfirmedAndWalletsUnchanged() {
        Booking booking = acceptedBooking();
        Wallet clientWallet = wallet(CLIENT, 10, NOW.minusSeconds(120));
        Wallet providerWallet = Wallet.open(PROVIDER);
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(clientWallet));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(providerWallet));

        assertThatThrownBy(() -> payBooking.pay(new PayBookingCommand(CLIENT, BOOKING_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
        assertThat(clientWallet.balance()).isEqualTo(new SkillCoins(10));
        assertThat(providerWallet.balance()).isEqualTo(SkillCoins.ZERO);
        verify(bookings, never()).save(eq(BOOKING_ID), any());
        verify(wallets, never()).save(any());
    }

    @Test
    void anUnrelatedUserCannotPayTheBooking() {
        Booking booking = acceptedBooking();
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> payBooking.pay(new PayBookingCommand(OUTSIDER, BOOKING_ID)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only the client can pay");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
        verify(wallets, never()).findByOwnerId(any());
    }

    @Test
    void cancellingAnAcceptedBookingDoesNotTouchWallets() {
        Booking booking = acceptedBooking();
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        SkillCoins refund = cancelBooking.cancel(new CancelBookingCommand(CLIENT, BOOKING_ID, "Cambio de planes"));

        assertThat(refund).isEqualTo(SkillCoins.ZERO);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookings).save(BOOKING_ID, booking);
        verify(wallets, never()).findByOwnerId(any());
    }

    @Test
    void cancellingAConfirmedBookingRefundsThroughTheWallets() {
        Booking booking = acceptedBooking();
        Wallet clientWallet = wallet(CLIENT, 100, NOW.minusSeconds(120));
        Wallet providerWallet = Wallet.open(PROVIDER);
        payments.pay(BOOKING_ID, clientWallet, providerWallet, PRICE, PAID_AT);
        booking.confirmPayment(CLIENT, PAID_AT);
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(clientWallet));
        when(wallets.findByOwnerId(PROVIDER)).thenReturn(Optional.of(providerWallet));

        SkillCoins refund = cancelBooking.cancel(new CancelBookingCommand(CLIENT, BOOKING_ID, "No puedo asistir"));

        assertThat(refund).isEqualTo(PRICE);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(clientWallet.balance()).isEqualTo(new SkillCoins(100));
        assertThat(providerWallet.balance()).isEqualTo(SkillCoins.ZERO);
        verify(bookings).save(BOOKING_ID, booking);
        verify(wallets).save(clientWallet);
        verify(wallets).save(providerWallet);
    }

    @Test
    void queryingOwnWalletReturnsItsBalanceAndLedger() {
        Wallet wallet = wallet(CLIENT, 75, NOW);
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(wallet));

        WalletView view = getWallet.getWallet(new GetWalletCommand(CLIENT, CLIENT));

        assertThat(view.ownerId()).isEqualTo(CLIENT);
        assertThat(view.balance()).isEqualTo(new SkillCoins(75));
        assertThat(view.movements()).hasSize(1);
    }

    @Test
    void aUserCannotQueryAnotherUsersWallet() {
        assertThatThrownBy(() -> getWallet.getWallet(new GetWalletCommand(CLIENT, PROVIDER)))
                .isInstanceOf(AccessDeniedException.class);

        verify(wallets, never()).findByOwnerId(any());
    }

    @Test
    void queryingAMissingWalletFailsExplicitly() {
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getWallet.getWallet(new GetWalletCommand(CLIENT, CLIENT)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Wallet not found");
    }

    private static Booking acceptedBooking() {
        Booking booking = Booking.request(10L, CLIENT, PROVIDER, SLOT, "Hola", "Aprender", PRICE, CREATED_AT);
        booking.accept(PROVIDER, CREATED_AT.plusSeconds(60));
        return booking;
    }

    private static Wallet wallet(UserId owner, long amount, Instant toppedUpAt) {
        Wallet wallet = Wallet.open(owner);
        wallet.topUp(new SkillCoins(amount), toppedUpAt);
        return wallet;
    }
}
