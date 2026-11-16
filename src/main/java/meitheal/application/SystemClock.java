package meitheal.application;

import java.time.Instant;
import java.time.LocalDate;

/** The production clock. */
public final class SystemClock implements Clock {

    @Override
    public Instant now() {
        return Instant.now();
    }

    @Override
    public LocalDate today() {
        return LocalDate.now();
    }
}
