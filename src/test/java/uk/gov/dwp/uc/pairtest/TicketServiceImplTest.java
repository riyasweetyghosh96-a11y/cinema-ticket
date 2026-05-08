package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class TicketServiceImplTest {

    private TicketPaymentService paymentService;
    private SeatReservationService reservationService;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        paymentService = Mockito.mock(TicketPaymentService.class);
        reservationService = Mockito.mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    @Test
    void shouldMakePaymentAndReserveSeatsForValidPurchase() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1));

        verify(paymentService).makePayment(1L, 40);
        verify(reservationService).reserveSeat(1L, 2);
        verifyNoMoreInteractions(paymentService, reservationService);
    }

    @Test
    void shouldReserveOnlyAdultAndChildSeats() {
        ticketService.purchaseTickets(123L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3));

        verify(paymentService).makePayment(123L, 95);
        verify(reservationService).reserveSeat(123L, 5);
    }

    @Test
    void shouldRejectChildTicketWithoutAdult() {
        assertInvalidPurchase(() -> ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1)),
                "Child and infant tickets require at least one adult ticket");
    }

    @Test
    void shouldRejectInfantTicketWithoutAdult() {
        assertInvalidPurchase(() -> ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)),
                "Child and infant tickets require at least one adult ticket");
    }

    @Test
    void shouldRejectMoreThanMaximumTickets() {
        TicketTypeRequest largePurchase = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);
        assertInvalidPurchase(() -> ticketService.purchaseTickets(1L, largePurchase),
                "Cannot purchase more than 25 tickets at a time");
    }

    @Test
    void shouldRejectNullAccountId() {
        assertInvalidPurchase(() -> ticketService.purchaseTickets(null,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)),
                "Account id must be greater than zero");
    }

    @Test
    void shouldRejectZeroAccountId() {
        assertInvalidPurchase(() -> ticketService.purchaseTickets(0L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)),
                "Account id must be greater than zero");
    }

    @Test
    void shouldRejectEmptyTicketRequests() {
        assertInvalidPurchase(() -> ticketService.purchaseTickets(1L),
                "At least one ticket request is required");
    }

    @ParameterizedTest
    @MethodSource("validPurchaseArguments")
    void shouldProcessValidPurchases(Long accountId,
                                     TicketTypeRequest[] requests,
                                     int expectedAmount,
                                     int expectedSeats) {
        ticketService.purchaseTickets(accountId, requests);

        verify(paymentService).makePayment(accountId, expectedAmount);
        verify(reservationService).reserveSeat(accountId, expectedSeats);
        verifyNoMoreInteractions(paymentService, reservationService);
    }

    private static Stream<Arguments> validPurchaseArguments() {
        return Stream.of(
                Arguments.of(1L,
                        new TicketTypeRequest[]{
                                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1),
                                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)
                        },
                        40,
                        2),
                Arguments.of(2L,
                        new TicketTypeRequest[]{
                                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 5)
                        },
                        125,
                        5),
                Arguments.of(3L,
                        new TicketTypeRequest[]{
                                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2)
                        },
                        25,
                        1)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidPurchaseArguments")
    void shouldRejectInvalidPurchases(Long accountId,
                                      TicketTypeRequest[] requests,
                                      String expectedMessage) {
        assertInvalidPurchase(() -> ticketService.purchaseTickets(accountId, requests), expectedMessage);
    }

    private static Stream<Arguments> invalidPurchaseArguments() {
        return Stream.of(
                Arguments.of(1L,
                        new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1)},
                        "Child and infant tickets require at least one adult ticket"),
                Arguments.of(1L,
                        new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)},
                        "Child and infant tickets require at least one adult ticket"),
                Arguments.of(null,
                        new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)},
                        "Account id must be greater than zero"),
                Arguments.of(1L,
                        new TicketTypeRequest[0],
                        "At least one ticket request is required"),
                Arguments.of(1L,
                        new TicketTypeRequest[]{new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26)},
                        "Cannot purchase more than 25 tickets at a time")
        );
    }

    private void assertInvalidPurchase(Executable executable, String expectedMessage) {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, executable);
        assertEquals(expectedMessage, exception.getMessage());
    }
}
