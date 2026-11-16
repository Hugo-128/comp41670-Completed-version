package meitheal.domain.ledger;

/**
 * The kind of a ledger entry. Every movement of credits is one of these, so the conservation rule (INV-3)
 * can be checked by type: exactly two kinds create credits (see R3), every other kind is a balanced
 * transfer between two existing accounts.
 */
public enum EntryType {

    /** R3: the one-off 120-credit welcome grant on registration. SYSTEM → member. */
    WELCOME_GRANT,

    /** R4: the session payment, written once both parties have confirmed. requester → provider. */
    TRANSFER,

    /** R10: the 5% community levy (rounded down, capped at headroom). requester → FUND. */
    LEVY,

    /** R11: a grant to a member in deficit. FUND → member. */
    FUND_GRANT,

    /** R14: a zero-amount record that a member cancelled less than 24h before the agreed start. */
    LATE_CANCELLATION
}
