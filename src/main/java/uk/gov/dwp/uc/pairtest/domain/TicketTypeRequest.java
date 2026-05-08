package uk.gov.dwp.uc.pairtest.domain;

/**
 * Immutable Object
 */
public class TicketTypeRequest {

    private final int noOfTickets;
    private final Type type;

    public TicketTypeRequest(Type type, int noOfTickets) {
        if (type == null) {
            throw new IllegalArgumentException("Ticket type must not be null");
        }
        if (noOfTickets <= 0) {
            throw new IllegalArgumentException("Number of tickets must be greater than zero");
        }
        this.type = type;
        this.noOfTickets = noOfTickets;
    }

    public int getNoOfTickets() {
        return noOfTickets;
    }

    public Type getTicketType() {
        return type;
    }

    public enum Type {
        ADULT, CHILD, INFANT
    }

}
