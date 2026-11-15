package com.uade.skillswap.application.usecases.booking;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.booking.BookingReceipt;
import com.uade.skillswap.application.dto.booking.GetBookingReceiptCommand;
import com.uade.skillswap.application.ports.out.BookingRepository;
import com.uade.skillswap.application.ports.out.wallet.WalletRepository;
import com.uade.skillswap.application.usecases.booking.GetBookingReceiptService;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.domain.wallet.BookingPaymentService;
import com.uade.skillswap.domain.wallet.CreditMovementType;
import com.uade.skillswap.domain.wallet.RefundPolicy;
import com.uade.skillswap.domain.wallet.TieredRefundStrategy;
import com.uade.skillswap.domain.wallet.Wallet;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingReceiptServiceTest {

    private static final UserId CLIENT = new UserId("ana");
    private static final UserId PROVIDER = new UserId("bruno");
    private static final UserId OUTSIDER = new UserId("carla");
    private static final long BOOKING_ID = 42L;
    private static final Instant PAID_AT = Instant.parse("2026-11-14T10:00:00Z");
    private static final SkillCoins PRICE = new SkillCoins(100);
    private static final TimeSlot SLOT = new TimeSlot(1L, Instant.parse("2026-11-19T21:00:00Z"),
            Instant.parse("2026-11-19T22:00:00Z"));

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final WalletRepository wallets = mock(WalletRepository.class);
    private final BookingPaymentService payments = new BookingPaymentService(
            new TieredRefundStrategy(new RefundPolicy(Duration.ofDays(2), Duration.ofDays(1))));
    private final GetBookingReceiptService service = new GetBookingReceiptService(bookings, wallets);

    @Test
    void receiptIsUnavailableUntilTheBookingIsCompleted() {
        Booking booking = confirmedBooking();
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.getReceipt(new GetBookingReceiptCommand(CLIENT, BOOKING_ID)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only for completed bookings");

        verify(wallets, never()).findByOwnerId(CLIENT);
    }

    @Test
    void completedBookingReceiptIncludesThePaymentAndCompletionTimes() {
        Booking booking = completedBooking();
        Wallet clientWallet = Wallet.open(CLIENT);
        clientWallet.topUp(PRICE, PAID_AT.minusSeconds(60));
        Wallet providerWallet = Wallet.open(PROVIDER);
        payments.pay(BOOKING_ID, clientWallet, providerWallet, PRICE, PAID_AT);
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(wallets.findByOwnerId(CLIENT)).thenReturn(Optional.of(clientWallet));

        BookingReceipt receipt = service.getReceipt(new GetBookingReceiptCommand(CLIENT, BOOKING_ID));

        assertThat(receipt.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(receipt.clientId()).isEqualTo("ana");
        assertThat(receipt.providerId()).isEqualTo("bruno");
        assertThat(receipt.amount()).isEqualTo(PRICE.amount());
        assertThat(receipt.paidAt()).isEqualTo(PAID_AT);
        assertThat(receipt.completedAt()).isEqualTo(booking.getHistory().get(booking.getHistory().size() - 1)
                .occurredAt());
        assertThat(clientWallet.movements()).filteredOn(m -> m.type() == CreditMovementType.DEBIT)
                .hasSize(1);
    }

    @Test
    void onlyBookingParticipantsCanSeeTheReceipt() {
        Booking booking = completedBooking();
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.getReceipt(new GetBookingReceiptCommand(OUTSIDER, BOOKING_ID)))
                .isInstanceOf(AccessDeniedException.class);

        verify(wallets, never()).findByOwnerId(CLIENT);
    }

    @Test
    void missingBookingHasAnExplicitNotFoundError() {
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReceipt(new GetBookingReceiptCommand(CLIENT, BOOKING_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found");
    }

    private static Booking confirmedBooking() {
        Booking booking = requestedBooking();
        booking.accept(PROVIDER);
        booking.confirmPayment(CLIENT);
        return booking;
    }

    private static Booking completedBooking() {
        Booking booking = confirmedBooking();
        booking.start(PROVIDER);
        booking.complete(PROVIDER);
        return booking;
    }

    private static Booking requestedBooking() {
        return Booking.request(10L, CLIENT, PROVIDER, SLOT, "Hola", "Aprender", PRICE);
    }
}
