package meitheal.domain.model;

/**
 * The length of a recorded session, in whole hours.
 *
 * <p>R8 says a recorded session must be longer than zero and no longer than 8 hours. Our team's reading
 * (see the Brightspace clarification we filed) restricts durations to whole hours, 1..8, rather than
 * inventing a rounding rule for fractional hours that the brief never specifies.
 *
 * <p>Enforces INV-6 (every engagement has 0 &lt; hours &le; 8) at the single point where a duration can
 * come into being. The test that pins this is {@code DurationTest.durationBoundaryOneAndEight}.
 */
public record Duration(int hours) {

    public static final int MIN_HOURS = 1;
    public static final int MAX_HOURS = 8;

    public Duration {
        if (hours < MIN_HOURS || hours > MAX_HOURS) {
            throw new IllegalArgumentException(
                    "session duration must be between " + MIN_HOURS + " and " + MAX_HOURS + " hours, got " + hours);
        }
    }

    /**
     * The flat cost of a session of this length, at 60 credits per hour (R1).
     * INV-5: no code path accepts a caller-supplied amount — the cost is always derived here.
     */
    public Credits cost() {
        return new Credits(hours * 60);
    }
}
