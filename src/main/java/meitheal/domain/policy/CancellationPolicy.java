package meitheal.domain.policy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * R14: cancelling a confirmed engagement <em>less than 24 hours</em> before its agreed start transfers no
 * credits, but is recorded against the cancelling member.
 *
 * <p>A pure decision function — no state, no clock reads. The "now" is injected so the rule is testable at
 * the exact 24h boundary (see {@code CancellationTest.cancelAtExactly24hIsNotLate}).
 *
 * <p>Convention: the "agreed start" is the start of {@code sessionDate} (00:00 UTC). A cancellation is
 * <em>late</em> when less than 24 hours remain before that instant.
 */
public final class CancellationPolicy {

    private CancellationPolicy() {
    }

    public static boolean isLateCancellation(LocalDate sessionDate, Instant now) {
        Instant start = sessionDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        long hoursUntilStart = ChronoUnit.HOURS.between(now, start);
        return hoursUntilStart < 24 && hoursUntilStart >= 0;
    }
}
