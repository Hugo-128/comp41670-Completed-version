package meitheal.domain.fund;

import meitheal.domain.model.Credits;

/**
 * The community fund, and the rules that govern it (R11).
 *
 * <p>INV-12 lives here: a FUND_GRANT is issued only when (a) the fund balance covers the amount — "the
 * fund cannot grant credits it does not hold", (b) the recipient's balance is below zero, and (c) the
 * recipient has no other FUND_GRANT in the same calendar month. The "same calendar month" check needs the
 * ledger history and a {@code Clock}, so {@code MeithealService} supplies {@code alreadyGrantedThisMonth};
 * this aggregate supplies the solvency and negativity judgement.
 *
 * <p>Like {@code Member}, this aggregate stores no balance: the fund balance is derived from the ledger
 * (INV-2) and passed in by the service.
 */
public final class CommunityFund {

    private CommunityFund() {
    }

    /** The outcome of an {@link #evaluate} call — approved, or rejected with a reason. */
    public record GrantDecision(boolean approved, String reason) {

        public static GrantDecision approve() {
            return new GrantDecision(true, "");
        }

        public static GrantDecision rejected(String reason) {
            return new GrantDecision(false, reason);
        }
    }

    /**
     * Decides whether the fund may grant {@code amount} to a recipient whose current balance is
     * {@code recipientBalance}, given a fund balance of {@code fundBalance} and whether the recipient has
     * already been granted this calendar month.
     */
    public static GrantDecision evaluate(Credits amount, int fundBalance, int recipientBalance,
                                         boolean alreadyGrantedThisMonth) {
        if (amount.value() > fundBalance) {
            return GrantDecision.rejected("fund cannot grant credits it does not hold");
        }
        if (recipientBalance >= 0) {
            return GrantDecision.rejected("recipient balance is not below zero");
        }
        if (alreadyGrantedThisMonth) {
            return GrantDecision.rejected("recipient already received a grant this calendar month");
        }
        return GrantDecision.approve();
    }
}
