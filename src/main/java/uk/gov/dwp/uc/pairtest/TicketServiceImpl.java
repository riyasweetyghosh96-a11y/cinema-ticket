package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Objects;

public class TicketServiceImpl implements TicketService {
    private static final int MAX_TICKETS = 25;
    private static final int ADULT_PRICE = 25;
    private static final int CHILD_PRICE = 15;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService,
                             SeatReservationService seatReservationService) {
        this.ticketPaymentService = Objects.requireNonNull(ticketPaymentService, "TicketPaymentService is required");
        this.seatReservationService = Objects.requireNonNull(seatReservationService, "SeatReservationService is required");
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validateAccount(accountId);
        validateTicketRequests(ticketTypeRequests);

        int adultCount = 0;
        int childCount = 0;
        int infantCount = 0;

        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            if (ticketTypeRequest == null) {
                throw new InvalidPurchaseException("Ticket request must not be null");
            }

            int quantity = ticketTypeRequest.getNoOfTickets();
            if (quantity <= 0) {
                throw new InvalidPurchaseException("Ticket quantity must be greater than zero");
            }

            switch (ticketTypeRequest.getTicketType()) {
                case ADULT -> adultCount += quantity;
                case CHILD -> childCount += quantity;
                case INFANT -> infantCount += quantity;
                default -> throw new InvalidPurchaseException("Unsupported ticket type");
            }
        }

        int totalTickets = adultCount + childCount + infantCount;
        if (totalTickets > MAX_TICKETS) {
            throw new InvalidPurchaseException("Cannot purchase more than " + MAX_TICKETS + " tickets at a time");
        }

        if (adultCount == 0 && (childCount > 0 || infantCount > 0)) {
            throw new InvalidPurchaseException("Child and infant tickets require at least one adult ticket");
        }

        int totalAmountToPay = (adultCount * ADULT_PRICE) + (childCount * CHILD_PRICE);
        int totalSeatsToAllocate = adultCount + childCount;

        ticketPaymentService.makePayment(accountId, totalAmountToPay);
        seatReservationService.reserveSeat(accountId, totalSeatsToAllocate);
    }

    private void validateAccount(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Account id must be greater than zero");
        }
    }

    private void validateTicketRequests(TicketTypeRequest... ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("At least one ticket request is required");
        }
    }
}
