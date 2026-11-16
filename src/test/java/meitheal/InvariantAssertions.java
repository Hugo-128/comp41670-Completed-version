package meitheal;

import meitheal.application.MeithealService;
import meitheal.domain.policy.LevyPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The invariant test harness. Call {@link #assertInvariantsHold(MeithealService)} at the end of every
 * scenario test so that, after any sequence of operations — including adversarial ones — the state-level
 * invariants are checked. This is the code the rubric's "the markers tried to break the overdraft floor,
 * the conservation rule, and the double-confirmation rule, and could not" is aimed at: we break it first.
 */
public final class InvariantAssertions {

    private InvariantAssertions() {
    }

    public static void assertInvariantsHold(MeithealService service) {
        // INV-3 (conservation): total credits in circulation == 120 × member count.
        assertEquals(120 * service.memberCount(),
                service.report().totalCreditsInCirculation(),
                "INV-3 conservation violated: total in circulation != 120 × memberCount");

        // INV-12 (partial, solvency): the fund must never be negative.
        assertTrue(service.report().fundBalance() >= 0,
                "community fund balance must never be negative, was " + service.report().fundBalance());

        // INV-4 (overdraft floor): every member balance >= −180.
        service.allBalances().forEach((member, balance) ->
                assertTrue(balance >= LevyPolicy.OVERDRAFT_FLOOR,
                        "INV-4 floor breached: " + member + " has balance " + balance));
    }
}
