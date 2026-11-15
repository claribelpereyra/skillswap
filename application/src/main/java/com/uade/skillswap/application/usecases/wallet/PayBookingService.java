package com.uade.skillswap.application.usecases.wallet;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.in.wallet.PayBookingCommand;
import com.uade.skillswap.application.ports.in.wallet.PayBookingUseCase;
import com.uade.skillswap.application.ports.out.TimeProvider;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.booking.BookingRepository;
import com.uade.skillswap.application.ports.out.wallet.WalletRepository;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.BookingPaymentService;
import com.uade.skillswap.domain.wallet.Wallet;

import java.time.Instant;
import java.util.Objects;

public final class PayBookingService implements PayBookingUseCase {

    private final BookingRepository bookings;
    private final WalletRepository wallets;
    private final BookingPaymentService payments;
    private final UnitOfWork unitOfWork;
    private final TimeProvider timeProvider;

    public PayBookingService(
            BookingRepository bookings,
            WalletRepository wallets,
            BookingPaymentService payments,
            UnitOfWork unitOfWork,
            TimeProvider timeProvider) {
        this.bookings = Objects.requireNonNull(bookings, "The booking repository is required");
        this.wallets = Objects.requireNonNull(wallets, "The wallet repository is required");
        this.payments = Objects.requireNonNull(payments, "The payment service is required");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "The unit of work is required");
        this.timeProvider = Objects.requireNonNull(timeProvider, "The time provider is required");
    }

    @Override
    public void pay(PayBookingCommand command) {
        unitOfWork.execute(() -> {
            Booking booking = bookings.findById(command.bookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", command.bookingId()));
            Instant paidAt = timeProvider.now();

            if (booking.getStatus() != BookingStatus.ACCEPTED) {
                booking.confirmPayment(command.actorId(), paidAt);
            }
            booking.requireClient(command.actorId(), "pay");
            requireNotBeforeLastTransition(booking, paidAt);

            Wallet clientWallet = findWallet(booking.getClientId());
            Wallet providerWallet = findWallet(booking.getProviderId());
            payments.pay(command.bookingId(), clientWallet, providerWallet, booking.getPrice(), paidAt);
            booking.confirmPayment(command.actorId(), paidAt);

            bookings.save(command.bookingId(), booking);
            wallets.save(clientWallet);
            wallets.save(providerWallet);
        });
    }

    private Wallet findWallet(UserId ownerId) {
        Wallet wallet = wallets.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", ownerId));
        if (!wallet.ownerId().equals(ownerId)) {
            throw new IllegalStateException("The wallet repository returned a wallet for another owner");
        }
        return wallet;
    }

    private static void requireNotBeforeLastTransition(Booking booking, Instant occurredAt) {
        StateTransition<?> lastTransition = booking.getHistory().get(booking.getHistory().size() - 1);
        if (occurredAt.isBefore(lastTransition.occurredAt())) {
            throw new IllegalStateException("A transition cannot be dated before the last recorded one");
        }
    }
}
