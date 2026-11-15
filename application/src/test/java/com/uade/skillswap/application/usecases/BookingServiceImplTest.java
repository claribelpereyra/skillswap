package com.uade.skillswap.application.usecases;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.BookingHistoryEntry;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.application.dto.RequestBookingCommand;
import com.uade.skillswap.application.ports.out.BookablePublication;
import com.uade.skillswap.application.ports.out.BookingRepository;
import com.uade.skillswap.application.ports.out.PublicationAvailability;
import com.uade.skillswap.application.ports.out.PublicationCatalog;
import com.uade.skillswap.application.ports.out.UnitOfWork;
import com.uade.skillswap.application.ports.out.wallet.WalletRepository;
import com.uade.skillswap.domain.booking.Booking;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.SkillCoins;
import com.uade.skillswap.domain.common.UserId;
import com.uade.skillswap.domain.publication.TimeSlot;
import com.uade.skillswap.domain.wallet.BookingPaymentService;
import com.uade.skillswap.domain.wallet.FullRefundStrategy;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        bookings = mock(BookingRepository.class);
        publications = mock(PublicationCatalog.class);
        availability = mock(PublicationAvailability.class);
        wallets = mock(WalletRepository.class);
        UnitOfWork direct = Supplier::get;
        service = new BookingServiceImpl(bookings, publications, availability, direct, wallets,
                new BookingPaymentService(new FullRefundStrategy()),
                Clock.fixed(Instant.parse("2026-11-14T10:00:00Z"), ZoneOffset.UTC));
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

    private static Booking pending() {
        return Booking.request(PUBLICATION_ID, CLIENT, PROVIDER, SLOT, "Hola", "Entrevista", new SkillCoins(100));
    }

    private static Booking accepted() {
        Booking booking = pending();
        booking.accept(PROVIDER);
        return booking;
    }

    private static Booking confirmed() {
        Booking booking = accepted();
        booking.confirmPayment(CLIENT);
        return booking;
    }
}
