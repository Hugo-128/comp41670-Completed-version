package meitheal.application;

import meitheal.domain.engagement.Engagement;
import meitheal.domain.ledger.LedgerEntry;
import meitheal.domain.member.Member;

import java.util.List;

/**
 * Persistence seam — <em>optional</em> per the brief (§6: a flat file is sufficient; a database earns
 * nothing).
 *
 * <p>{@link MeithealService} holds all state in memory; it works end-to-end without any repository. This
 * interface is the extension point the member who owns CLI/persistence implements as a JSON flat-file
 * version and wires into {@code Main} at startup/shutdown. The exact serialisation contract is that
 * member's design decision — this interface simply names the boundary so the rest of the system never
 * needs to know how state is stored.
 *
 * <p>TODO (owner E): define {@code save}/{@code load} over the domain state (members, engagements, ledger)
 * and a JSON implementation.
 */
public interface MeithealRepository {

    /** Persist the domain state. */
    void save(List<Member> members, List<Engagement> engagements, List<LedgerEntry> ledgerEntries);

    /** A snapshot of the domain state, for reloading. */
    record State(List<Member> members, List<Engagement> engagements, List<LedgerEntry> ledgerEntries) {
    }
}
