package meitheal.domain.engagement;

/**
 * The lifecycle of an engagement.
 *
 * <p>PROPOSED → (accepted) → ACTIVE → (second distinct confirmation) → SETTLED.
 * From PROPOSED or ACTIVE it may also reach CANCELLED (in time) or CANCELLED_LATE (less than 24h before
 * the agreed start, R14). SETTLED is terminal and idempotent — see INV-8.
 */
public enum EngagementState {
    PROPOSED,
    ACTIVE,
    SETTLED,
    CANCELLED,
    CANCELLED_LATE;

    public boolean isTerminal() {
        return this == SETTLED || this == CANCELLED || this == CANCELLED_LATE;
    }
}
