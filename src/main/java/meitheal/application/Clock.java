package meitheal.application;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A seam for time. Two rules need the current time — the "same calendar month" check (R11) and the 24-hour
 * late-cancellation check (R14) — and neither may read the wall clock directly, or the tests could not pin
 * the boundary. Inject a {@code SystemClock} in production and a fixed clock in tests.
 */
public interface Clock {

    Instant now();

    LocalDate today();
}
