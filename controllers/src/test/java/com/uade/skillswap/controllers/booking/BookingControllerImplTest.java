package com.uade.skillswap.controllers.booking;

import com.uade.skillswap.application.common.AccessDeniedException;
import com.uade.skillswap.application.common.ConcurrentUpdateException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.common.ResourceNotFoundException;
import com.uade.skillswap.application.dto.BookingHistoryEntry;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.application.dto.RequestBookingCommand;
import com.uade.skillswap.application.ports.in.BookingService;
import com.uade.skillswap.application.ports.out.IdentityService;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.ControllerResult.ErrorType;
import com.uade.skillswap.domain.booking.BookingStatus;
import com.uade.skillswap.domain.common.BusinessRuleException;
import com.uade.skillswap.domain.common.InvalidTransitionException;
import com.uade.skillswap.domain.common.PermissionDeniedException;
import com.uade.skillswap.domain.common.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BookingControllerImplTest {

    private static final UserId ANA = new UserId("ana");
    private static final UserId BRUNO = new UserId("bruno");
    private static final BookingView VIEW = new BookingView(99L, 10L, "ana", "bruno", 1L,
            Instant.parse("2026-11-19T21:00:00Z"), Instant.parse("2026-11-19T22:00:00Z"),
            BookingStatus.PENDING, 100, "Hola", "Entrevista", null, null);

    private BookingService service;
    private BookingControllerImpl controller;
    private UserId loggedUser;

    @BeforeEach
    void setUp() {
        service = mock(BookingService.class);
        IdentityService identity = () -> loggedUser;
        controller = new BookingControllerImpl(service, identity);
    }

    @Test
    void requestConvertsTheInputAndReturnsTheBooking() {
        loggedAs(ANA);
        when(service.request(new RequestBookingCommand(ANA, 10L, 1L, "Hola", "Entrevista"))).thenReturn(VIEW);

        ControllerResult<BookingView> result = controller.request("ana", 10L, 1L, "Hola", "Entrevista");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(VIEW);
        assertThat(result.getErrorType()).isNull();
    }

    @Test
    void everyActionDelegatesWithTheUserAndTheBooking() {
        when(service.accept(99L, BRUNO)).thenReturn(VIEW);
        when(service.reject(99L, BRUNO, "No")).thenReturn(VIEW);
        when(service.start(99L, BRUNO)).thenReturn(VIEW);
        when(service.complete(99L, ANA)).thenReturn(VIEW);
        when(service.proposeReschedule(99L, BRUNO, 2L)).thenReturn(VIEW);
        when(service.acceptReschedule(99L, ANA)).thenReturn(VIEW);
        when(service.rejectReschedule(99L, ANA)).thenReturn(VIEW);

        loggedAs(BRUNO);
        List<ControllerResult<BookingView>> asProvider = List.of(
                controller.accept("bruno", 99L),
                controller.reject("bruno", 99L, "No"),
                controller.start("bruno", 99L),
                controller.proposeReschedule("bruno", 99L, 2L));
        loggedAs(ANA);
        List<ControllerResult<BookingView>> asClient = List.of(
                controller.complete("ana", 99L),
                controller.acceptReschedule("ana", 99L),
                controller.rejectReschedule("ana", 99L));

        assertThat(asProvider).allSatisfy(result -> assertThat(result.getData()).isEqualTo(VIEW));
        assertThat(asClient).allSatisfy(result -> assertThat(result.getData()).isEqualTo(VIEW));
    }

    @Test
    void aBrokenBusinessRuleIsReturnedAsAnError() {
        when(service.accept(99L, ANA)).thenThrow(new BusinessRuleException("The slot is already taken"));
        when(service.start(99L, BRUNO)).thenThrow(new InvalidTransitionException(BookingStatus.PENDING, "start"));

        loggedAs(ANA);
        ControllerResult<BookingView> slotTaken = controller.accept("ana", 99L);
        loggedAs(BRUNO);
        ControllerResult<BookingView> wrongState = controller.start("bruno", 99L);

        assertThat(slotTaken.isSuccess()).isFalse();
        assertThat(slotTaken.getErrorType()).isEqualTo(ErrorType.BUSINESS_RULE);
        assertThat(slotTaken.getMessage()).isEqualTo("The slot is already taken");
        assertThat(wrongState.getErrorType()).isEqualTo(ErrorType.BUSINESS_RULE);
        assertThat(wrongState.getMessage()).isEqualTo("Cannot start in state PENDING");
    }

    @Test
    void accessDeniedAndNotFoundHaveTheirOwnErrorType() {
        when(service.history(99L, ANA)).thenThrow(new AccessDeniedException(ANA, "view the history"));
        when(service.accept(7L, BRUNO)).thenThrow(new ResourceNotFoundException("Booking", 7L));

        loggedAs(ANA);
        assertThat(controller.history("ana", 99L).getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        loggedAs(BRUNO);
        assertThat(controller.accept("bruno", 7L).getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
    }

    @Test
    void aDomainPermissionDeniedIsReturnedAsAccessDenied() {
        loggedAs(ANA);
        when(service.accept(99L, ANA)).thenThrow(new PermissionDeniedException("Only the provider can accept a booking"));

        ControllerResult<BookingView> result = controller.accept("ana", 99L);

        assertThat(result.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(result.getMessage()).isEqualTo("Only the provider can accept a booking");
    }

    @Test
    void aConcurrentUpdateIsReturnedAsAConflictWithItsMessage() {
        loggedAs(BRUNO);
        ConcurrentUpdateException conflict = new ConcurrentUpdateException(new RuntimeException("version mismatch"));
        when(service.accept(99L, BRUNO)).thenThrow(conflict);

        ControllerResult<BookingView> result = controller.accept("bruno", 99L);

        assertThat(result.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.getMessage()).isEqualTo(conflict.getMessage());
    }

    @Test
    void theUserMustBeTheLoggedOneBeforeCallingTheService() {
        loggedAs(ANA);

        ControllerResult<BookingView> asSomeoneElse = controller.accept("bruno", 99L);
        ControllerResult<Page<BookingView>> listAsSomeoneElse = controller.listAsClient("bruno", 0, 10);

        assertThat(asSomeoneElse.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(asSomeoneElse.getMessage()).isEqualTo("User bruno is not allowed to accept a booking");
        assertThat(listAsSomeoneElse.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        verifyNoInteractions(service);
    }

    @Test
    void withoutAnActiveUserTheAccessIsDenied() {
        loggedAs(ANA);

        ControllerResult<BookingView> nullUser = controller.accept(null, 99L);
        ControllerResult<BookingView> blankUser = controller.accept(" ", 99L);

        assertThat(nullUser.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        assertThat(nullUser.getMessage()).isEqualTo("No active user");
        assertThat(blankUser.getErrorType()).isEqualTo(ErrorType.ACCESS_DENIED);
        verifyNoInteractions(service);
    }

    @Test
    void invalidInputIsRejectedBeforeCallingTheService() {
        loggedAs(ANA);
        ControllerResult<Page<BookingView>> badPage = controller.listAsClient("ana", -1, 20);

        assertThat(badPage.getErrorType()).isEqualTo(ErrorType.INVALID_INPUT);
        verifyNoInteractions(service);
    }

    @Test
    void anUnexpectedErrorDoesNotShowTechnicalDetails() {
        loggedAs(ANA);
        when(service.complete(99L, ANA)).thenThrow(new IllegalStateException("Connection to localhost:3306 refused"));

        ControllerResult<BookingView> result = controller.complete("ana", 99L);

        assertThat(result.getErrorType()).isEqualTo(ErrorType.UNEXPECTED);
        assertThat(result.getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void historyAndListsReturnTheServiceData() {
        loggedAs(ANA);
        BookingHistoryEntry created = new BookingHistoryEntry(null, BookingStatus.PENDING, "ana", Instant.now(), null);
        PageRequest page = new PageRequest(0, 10);
        when(service.history(99L, ANA)).thenReturn(List.of(created));
        when(service.listAsClient(ANA, page)).thenReturn(new Page<>(List.of(VIEW), page, 1));
        when(service.listAsProvider(any(), any())).thenReturn(Page.empty(page));

        assertThat(controller.history("ana", 99L).getData()).containsExactly(created);
        assertThat(controller.listAsClient("ana", 0, 10).getData().items()).containsExactly(VIEW);
        assertThat(controller.listAsProvider("ana", 0, 10).getData().items()).isEmpty();
        verify(service).listAsProvider(ANA, page);
    }

    private void loggedAs(UserId user) {
        loggedUser = user;
    }
}
