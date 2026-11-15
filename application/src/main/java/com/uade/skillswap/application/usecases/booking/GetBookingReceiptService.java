package com.uade.skillswap.application.usecases.booking;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.booking.BookingReceipt;
import com.uade.skillswap.application.dto.booking.GetBookingReceiptCommand;
import com.uade.skillswap.application.ports.in.booking.GetBookingReceiptUseCase;
import com.uade.skillswap.application.ports.out.BookingRepository;
import com.uade.skillswap.application.ports.out.wallet.WalletRepository;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.StateTransition;
import com.uade.skillswap.domain.wallet.CreditMovement;
import com.uade.skillswap.domain.wallet.CreditMovementType;
import com.uade.skillswap.domain.wallet.Wallet;

import java.time.Instant;
import java.util.Objects;

public final class GetBookingReceiptService implements GetBookingReceiptUseCase {

    private final BookingRepository bookings;
    private final WalletRepository wallets;

    public GetBookingReceiptService(BookingRepository bookings, WalletRepository wallets) {
        this.bookings = Objects.requireNonNull(bookings, "The booking repository is required");
        this.wallets = Objects.requireNonNull(wallets, "The wallet repository is required");
    }

    @Override
    public BookingReceipt getReceipt(GetBookingReceiptCommand command) {
        Booking booking = bookings.findById(command.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", command.bookingId()));
        if (!booking.isParticipant(command.actorId())) {
            throw new AccessDeniedException(command.actorId(), "view this booking receipt");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessRuleException("A receipt is available only for completed bookings");
        }

        Wallet clientWallet = wallets.findByOwnerId(booking.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", booking.getClientId()));
        CreditMovement payment = clientWallet.movements().stream()
                .filter(movement -> movement.type() == CreditMovementType.DEBIT
                        && Objects.equals(movement.bookingId(), command.bookingId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Payment", command.bookingId()));
        if (payment.amount().amount() > booking.getPrice().amount()) {
            throw new IllegalStateException("The recorded payment exceeds the booking price");
        }
        Instant completedAt = booking.getHistory().stream()
                .filter(transition -> transition.to() == BookingStatus.COMPLETED)
                .map(StateTransition::occurredAt)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "A completed booking must have a completion transition"));

        return new BookingReceipt(command.bookingId(), booking.getClientId().value(), booking.getProviderId().value(),
                payment.amount().amount(), payment.occurredAt(), completedAt);
    }
}
