package com.uade.skillswap.domain.booking.state;

import com.uade.skillswap.domain.booking.BookingStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BookingStateFactoryTest {

    @Test
    void rebuildsTheStateForEveryStatus() {
        Map<BookingStatus, Class<? extends BookingState>> expected = Map.of(
                BookingStatus.PENDING, PendingState.class,
                BookingStatus.ACCEPTED, AcceptedState.class,
                BookingStatus.CONFIRMED, ConfirmedState.class,
                BookingStatus.IN_PROGRESS, InProgressState.class,
                BookingStatus.COMPLETED, CompletedState.class,
                BookingStatus.REJECTED, RejectedState.class,
                BookingStatus.CANCELLED, CancelledState.class);

        for (BookingStatus status : BookingStatus.values()) {
            BookingState state = BookingStateFactory.fromStatus(status);

            assertThat(state).isInstanceOf(expected.get(status));
            assertThat(state.getStatus()).isEqualTo(status);
        }
    }
}
