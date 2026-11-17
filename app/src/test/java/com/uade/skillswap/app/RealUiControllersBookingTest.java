package com.uade.skillswap.app;

import com.uade.skillswap.app.presentation.ApplicationControllers;
import com.uade.skillswap.app.presentation.RealUiControllers;
import com.uade.skillswap.app.presentation.UiOperationException;
import com.uade.skillswap.application.common.Page;
import com.uade.skillswap.application.common.PageRequest;
import com.uade.skillswap.application.dto.BookingView;
import com.uade.skillswap.controllers.ControllerResult;
import com.uade.skillswap.controllers.booking.BookingController;
import com.uade.skillswap.controllers.session.SessionController;
import com.uade.skillswap.domain.booking.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RealUiControllersBookingTest {
    private final BookingController bookings = mock(BookingController.class);
    private final SessionController session = mock(SessionController.class);
    private RealUiControllers ui;

    @BeforeEach void preparar() {
        var app = new ApplicationControllers(session, null, null, null, null, null, bookings,
                null, null, null, null, null, null);
        when(session.currentUserId()).thenReturn("ana");
        ui = new RealUiControllers(app, "ana", ZoneOffset.UTC);
    }

    private static BookingView booking(long id, Long proposedSlot) {
        return new BookingView(id, 10, "ana", "bruno", 1,
                Instant.parse("2026-12-01T10:00:00Z"), Instant.parse("2026-12-01T11:00:00Z"),
                BookingStatus.ACCEPTED, 100, "Hola", "Aprender", proposedSlot,
                proposedSlot == null ? null : "bruno", "Mentoría Java", "Ana", "Bruno");
    }

    @Test void renderizarVeinteFilasNoVuelveAConsultarCadaContratacion() {
        var client = LongStream.rangeClosed(1, 12).mapToObj(i -> booking(i, i == 1 ? 2L : null)).toList();
        var provider = LongStream.rangeClosed(13, 20).mapToObj(i -> booking(i, null)).toList();
        when(bookings.listAsClient("ana", 0, 12)).thenReturn(ControllerResult.success(new Page<>(client, new PageRequest(0, 12), 12)));
        when(bookings.listAsProvider("ana", 0, 12)).thenReturn(ControllerResult.success(new Page<>(provider, new PageRequest(0, 12), 8)));

        var rows = ui.listarContrataciones("ana");

        assertThat(rows).hasSize(20);
        for (var row : rows) assertThat(ui.tieneReprogramacion("ana", row.id())).isEqualTo(row.id().equals("1"));
        verify(bookings).listAsClient("ana", 0, 12);
        verify(bookings).listAsProvider("ana", 0, 12);
        verifyNoMoreInteractions(bookings);
        // El snapshot nunca saltea la validación de sesión.
        when(session.currentUserId()).thenReturn("bruno");
        assertThatThrownBy(() -> ui.tieneReprogramacion("ana", "1")).isInstanceOf(UiOperationException.class);
        verifyNoMoreInteractions(bookings);
    }

    @Test void unaAccionReleeLaReservaAunqueHayaUnListadoAnterior() {
        var pending = new BookingView(1L, 10, "ana", "bruno", 1,
                Instant.parse("2026-12-01T10:00:00Z"), Instant.parse("2026-12-01T11:00:00Z"),
                BookingStatus.PENDING, 100, "Hola", "Aprender", null, null, "Mentoría Java", "Ana", "Bruno");
        when(bookings.listAsClient("ana", 0, 12)).thenReturn(ControllerResult.success(new Page<>(List.of(pending), new PageRequest(0, 12), 1)));
        when(bookings.listAsProvider("ana", 0, 12)).thenReturn(ControllerResult.success(Page.empty(new PageRequest(0, 12))));
        ui.listarContrataciones("ana");
        when(bookings.get("ana", 1)).thenReturn(ControllerResult.success(booking(1, null)));
        when(bookings.pay("ana", 1)).thenReturn(ControllerResult.success(booking(1, null)));

        ui.avanzar("ana", "1");

        verify(bookings).get("ana", 1);
        verify(bookings).pay("ana", 1);
        verify(bookings, never()).accept(anyString(), anyLong());
        verify(bookings, never()).listAsClient(anyString(), anyInt(), eq(100));
        verify(bookings, never()).listAsProvider(anyString(), anyInt(), eq(100));
    }

    @Test void sinListadoLaReprogramacionSeConsultaPorId() {
        when(bookings.get("ana", 1)).thenReturn(ControllerResult.success(booking(1, 2L)));
        assertThat(ui.tieneReprogramacion("ana", "1")).isTrue();
        verify(bookings).get("ana", 1);
        verifyNoMoreInteractions(bookings);
    }
}
