package com.uade.skillswap.application.usecases.wallet;

import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.ports.in.wallet.CancelBookingCommand;
import com.uade.skillswap.application.ports.in.wallet.CancelBookingUseCase;
import com.uade.skillswap.application.ports.out.TimeProvider;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.booking.BookingRepository;
import com.uade.skillswap.application.ports.out.wallet.WalletRepository;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.wallet.BookingPaymentService;
import com.uade.skillswap.domain.wallet.Wallet;

import java.time.Instant;
import java.util.Objects;

public final class CancelBookingService implements CancelBookingUseCase {

    private final BookingRepository bookings;
    private final WalletRepository wallets;
    private final BookingPaymentService payments;
    private final UnitOfWork unitOfWork;
    private final TimeProvider timeProvider;

    public CancelBookingService(
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
    public SkillCoins cancel(CancelBookingCommand command) {
        return unitOfWork.execute(() -> {
            Booking booking = bookings.findById(command.bookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", command.bookingId()));
            Instant cancelledAt = timeProvider.now();
            BookingStatus status = booking.getStatus();

            if (status != BookingStatus.ACCEPTED && status != BookingStatus.CONFIRMED) {
                booking.cancel(command.actorId(), command.reason(), cancelledAt);
            }
            booking.prepareCancellation(command.actorId(), command.reason());
            requireNotBeforeLastTransition(booking, cancelledAt);

            SkillCoins refund = SkillCoins.ZERO;
            Wallet clientWallet = null;
            Wallet providerWallet = null;
            if (status == BookingStatus.CONFIRMED) {
                clientWallet = findWallet(booking.getClientId());
                providerWallet = findWallet(booking.getProviderId());
                refund = payments.refund(command.bookingId(), clientWallet, providerWallet,
                        booking.getSlot().getStartsAt(), cancelledAt);
            }

            booking.cancel(command.actorId(), command.reason(), cancelledAt);
            bookings.save(command.bookingId(), booking);
            if (status == BookingStatus.CONFIRMED) {
                wallets.save(clientWallet);
                wallets.save(providerWallet);
            }
            return refund;
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
