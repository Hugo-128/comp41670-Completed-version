package meitheal.domain.ledger;

import meitheal.domain.model.MemberId;

/**
 * A party that can hold credits on the ledger.
 *
 * <p>Three account kinds exist. {@code MemberAccount} is a real member. {@code FundAccount} is the
 * community fund (R10 levy accumulates here; R11 grants leave here). {@code SystemAccount} is the source
 * of the welcome grant (R3) — it exists so that the welcome grant is also a balanced transfer rather than
 * an unaccounted credit, which is what makes the conservation check trivial: the sum over every account is
 * always zero.
 *
 * <p>Sealed so that a {@code switch} over accounts is exhaustive — the compiler flags a forgotten case.
 */
public sealed interface Account {

    String label();

    record MemberAccount(MemberId memberId) implements Account {
        public MemberAccount {
            if (memberId == null) {
                throw new IllegalArgumentException("memberId must not be null");
            }
        }

        @Override
        public String label() {
            return memberId.value();
        }
    }

    record FundAccount() implements Account {
        @Override
        public String label() {
            return "FUND";
        }
    }

    record SystemAccount() implements Account {
        @Override
        public String label() {
            return "SYSTEM";
        }
    }
}
