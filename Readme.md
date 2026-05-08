# Cinema Ticket Purchase Service

This project implements a `TicketService` for cinema ticket purchases, following business rules for ticket pricing, seat reservation, and purchase validation.

## Business rules implemented

- Supported ticket types: `INFANT`, `CHILD`, `ADULT`
- Pricing:
  - `INFANT`: £0
  - `CHILD`: £15
  - `ADULT`: £25
- Maximum of `25` tickets may be purchased per transaction
- `CHILD` and `INFANT` tickets cannot be purchased without at least one `ADULT` ticket
- `INFANT` tickets do not reserve seats
- Payment is processed via `TicketPaymentService`
- Seat reservation is performed via `SeatReservationService`

## Implementation details

- `src/main/java/uk/gov/dwp/uc/pairtest/TicketServiceImpl.java`
  - Validates account ID and request payload
  - Calculates total payment amount and seats to reserve
  - Uses injected third-party services for payment and reservation
- `src/main/java/uk/gov/dwp/uc/pairtest/domain/TicketTypeRequest.java`
  - Immutable request object with validation on creation
- `src/main/java/uk/gov/dwp/uc/pairtest/exception/InvalidPurchaseException.java`
  - Runtime exception raised for invalid purchase scenarios

## Tests

- `src/test/java/uk/gov/dwp/uc/pairtest/TicketServiceImplTest.java`
  - Uses JUnit 5 and Mockito
  - Includes parameterized tests covering valid purchase scenarios and invalid input combinations

## Build and run tests

Use Maven to compile and execute tests:

```bash
mvn test
```
