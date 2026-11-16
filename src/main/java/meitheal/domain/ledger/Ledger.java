package meitheal.domain.ledger;

import meitheal.domain.model.Credits;
import meitheal.domain.model.MemberId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The append-only ledger of every credit movement. The aggregate root that holds the system's money.
 *
 * <p>Two invariants live here:
 * <ul>
 *   <li><b>INV-1</b> (R7): append-only. The backing list is never truncated and no entry is ever edited or
 *       removed; a correction is a new compensating entry. {@link #entries()} returns an immutable copy, so
 *       no caller can reach the backing list (also R12).</li>
 *   <li><b>INV-2</b> (R3, R7): balances are <em>derived</em>, never stored. There is no {@code balance}
 *       field anywhere in the system; {@link #balanceOf} sums the entries on demand. A parallel stored
 *       balance that can drift from the ledger is exactly the bug the conservation rule exists to catch.</li>
 * </ul>
 *
 * <p>Conservation (INV-3) is checked against the whole ledger by {@code MeithealService.report()}, not
 * here — it spans every member plus the fund, which only the service knows how to enumerate.
 *
 * <p>Threading note: the brief targets a single-threaded command-line system; this class is not
 * synchronised. Concurrency is a known limitation, recorded in the Design Report §6.
 */
public final class Ledger {

    private final List<LedgerEntry> entries = new ArrayList<>();

    /** Appends an entry and returns it. The only way a credit movement enters the ledger. */
    public LedgerEntry append(EntryType type, Account from, Account to, Credits amount,
                              Instant recordedAt, String note) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(recordedAt, "recordedAt");

        int sequence = entries.size();
        LedgerEntry entry = new LedgerEntry(sequence, type, from, to, amount, recordedAt, note);
        entries.add(entry);
        return entry;
    }

    /** Net balance of one member: credits in − debits out (INV-2). May be negative, down to −180. */
    public int balanceOf(MemberId memberId) {
        return balanceOf(new Account.MemberAccount(memberId));
    }

    /** Net balance of any account, including the fund and the system accounts. */
    public int balanceOf(Account account) {
        Objects.requireNonNull(account, "account");
        int balance = 0;
        for (LedgerEntry e : entries) {
            balance += effectOnAccount(e, account);
        }
        return balance;
    }

    private int effectOnAccount(LedgerEntry e, Account account) {
        if (e.to().equals(account)) {
            return e.amount().value();
        }
        if (e.from().equals(account)) {
            return -e.amount().value();
        }
        return 0;
    }

    /** The community fund's balance (levy in, grants out). */
    public int fundBalance() {
        return balanceOf(new Account.FundAccount());
    }

    /**
     * The number of welcome grants recorded — each is 120 credits, so this × 120 is the amount the system
     * account has issued. Used by the conservation check.
     */
    public long welcomeGrantCount() {
        return entries.stream().filter(e -> e.type() == EntryType.WELCOME_GRANT).count();
    }

    /** An immutable snapshot of the ledger (R12: no caller can mutate the internal list). */
    public List<LedgerEntry> entries() {
        return List.copyOf(entries);
    }

    public int size() {
        return entries.size();
    }
}
