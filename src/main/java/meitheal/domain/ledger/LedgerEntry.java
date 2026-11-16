package meitheal.domain.ledger;

import meitheal.domain.model.Credits;

import java.time.Instant;
import java.util.Objects;

/**
 * One append-only entry in the ledger (R7).
 *
 * <p>Immutable by construction — a {@code record} whose components are themselves immutable — so INV-1
 * (an entry written at index {@code i} is byte-identical in every later state) holds by construction
 * rather than by discipline. There is deliberately no setter and no way to edit a field.
 *
 * <p>Every entry is a balanced movement from {@code from} to {@code to}: the {@code from} account is
 * debited {@code amount} and the {@code to} account is credited {@code amount}. {@code LATE_CANCELLATION}
 * entries carry a zero amount and exist only as an audit record (INV-14).
 */
public record LedgerEntry(
        int sequence,
        EntryType type,
        Account from,
        Account to,
        Credits amount,
        Instant recordedAt,
        String note) {

    public LedgerEntry {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(recordedAt, "recordedAt");
        if (from.equals(to)) {
            throw new IllegalArgumentException("a ledger entry must move credits between two distinct accounts");
        }
    }
}
