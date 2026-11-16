package meitheal.domain.model;

import java.time.LocalDate;

/**
 * The window in which a request must be fulfilled, expressed as an earliest and a latest date.
 *
 * <p>R9: an engagement against a request cannot be confirmed for a date outside this window. Our reading
 * (filed on Brightspace) is that the constrained date is the <em>session date</em>, not the calendar day
 * on which the confirmation action happens.
 *
 * <p>Enforces INV-11 (window.from &le; sessionDate &le; window.to) via {@link #contains(LocalDate)}.
 */
public record FulfilmentWindow(LocalDate earliest, LocalDate latest) {

    public FulfilmentWindow {
        if (earliest.isAfter(latest)) {
            throw new IllegalArgumentException(
                    "fulfilment window earliest (" + earliest + ") must not be after latest (" + latest + ")");
        }
    }

    /** True when {@code sessionDate} falls inside this window (inclusive of both ends). */
    public boolean contains(LocalDate sessionDate) {
        return !sessionDate.isBefore(earliest) && !sessionDate.isAfter(latest);
    }
}
