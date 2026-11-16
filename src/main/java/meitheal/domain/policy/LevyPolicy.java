package meitheal.domain.policy;

import meitheal.domain.model.Credits;

/**
 * R10: a community levy of 5% is taken from each transfer and credited to the community fund, rounded
 * <em>down</em> to whole credits, paid by the <em>requester</em> on top of the session cost, and never the
 * thing that pushes the requester below the overdraft floor.
 *
 * <p>Team decision D-b (Design Report §1.2, secondary tension): when the session cost alone lands the
 * requester at or near the −180 floor there is no headroom for the full 5%, so we <em>cap</em> the levy at
 * the available headroom rather than reject the whole transfer:
 *
 * <pre>levy = min( ⌊ cost × 0.05 ⌋, headroom ),  headroom = (requester balance after cost) − (−180)</pre>
 *
 * <p>This is a pure function on {@link Credits}. The service calls it once from {@code confirmSession};
 * the result is written as a ledger entry <em>even when it is zero</em>, so the audit trail never has a
 * hole (INV-7). Round-down uses integer division, deliberately not {@code Math.round}.
 */
public final class LevyPolicy {

    public static final double LEVY_RATE = 0.05;
    public static final int OVERDRAFT_FLOOR = -180;

    private LevyPolicy() {
    }

    /**
     * The full 5% levy on {@code cost}, rounded down to whole credits.
     */
    public static Credits fullLevy(Credits cost) {
        int roundedDown = (int) (cost.value() * LEVY_RATE); // truncation == round-down for non-negative ints
        return new Credits(roundedDown);
    }

    /**
     * The levy actually charged, capped at the headroom left after {@code cost} is debited from
     * {@code requesterBalanceAfterCost}. Never pushes the requester below {@link #OVERDRAFT_FLOOR}.
     */
    public static Credits levy(Credits cost, int requesterBalanceAfterCost) {
        Credits full = fullLevy(cost);
        int headroom = requesterBalanceAfterCost - OVERDRAFT_FLOOR; // ≥ 0 when the floor is respected
        int capped = Math.max(0, Math.min(full.value(), headroom));
        return new Credits(capped);
    }
}
