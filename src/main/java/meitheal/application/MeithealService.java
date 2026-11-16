package meitheal.application;

import meitheal.domain.catalogue.SkillCatalogue;
import meitheal.domain.engagement.Engagement;
import meitheal.domain.engagement.EngagementState;
import meitheal.domain.fund.CommunityFund;
import meitheal.domain.ledger.Account;
import meitheal.domain.ledger.EntryType;
import meitheal.domain.ledger.Ledger;
import meitheal.domain.ledger.LedgerEntry;
import meitheal.domain.member.Member;
import meitheal.domain.model.CategoryName;
import meitheal.domain.model.Credits;
import meitheal.domain.model.Duration;
import meitheal.domain.model.Email;
import meitheal.domain.model.FulfilmentWindow;
import meitheal.domain.model.MemberId;
import meitheal.domain.policy.LevyPolicy;
import meitheal.domain.request.Offer;
import meitheal.domain.request.Request;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The application service: the single write path into the system, and the only place business rules that
 * span more than one aggregate are decided.
 *
 * <p>This is where the cross-aggregate invariants live, because no aggregate root can own them alone:
 * <ul>
 *   <li><b>INV-3 (conservation)</b> — the welcome grant is the one credit-creation event, written here as
 *       SYSTEM → member; every other write is a balanced transfer between two existing accounts.</li>
 *   <li><b>INV-7 (levy)</b> — computed here via {@link LevyPolicy} and written even when zero.</li>
 *   <li><b>INV-10 (suspension)</b> — the status gate sits on the three <em>creation</em> methods only
 *       ({@link #publishOffer}, {@link #raiseRequest}, {@link #proposeEngagement}), never on settlement
 *       (decision D-a).</li>
 *   <li><b>INV-12 (fund grant)</b> — the month + negativity + solvency judgement via {@link CommunityFund}.</li>
 * </ul>
 *
 * <p>All state is held in memory. Persistence is an optional extension (flat file is sufficient per the
 * brief) and is deliberately left out of the core: see {@link MeithealRepository} for the seam.
 */
public final class MeithealService {

    private final SkillCatalogue catalogue;
    private final Clock clock;
    private final Ledger ledger = new Ledger();
    private final Map<MemberId, Member> members = new LinkedHashMap<>();
    private final List<Engagement> engagements = new ArrayList<>();

    public MeithealService(SkillCatalogue catalogue, Clock clock) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // --- §5 operations ---------------------------------------------------

    /** Register a member with a name and email; ACTIVE status; the 120-credit welcome grant (R3). */
    public MemberId registerMember(String name, Email email) {
        MemberId id = new MemberId("M" + (members.size() + 1));
        Member member = new Member(id, name, email);
        members.put(id, member);

        // R3: the welcome grant is the one credit-creation event.
        ledger.append(EntryType.WELCOME_GRANT,
                new Account.SystemAccount(),
                new Account.MemberAccount(id),
                new Credits(120),
                clock.now(),
                "welcome grant for " + name);
        return id;
    }

    /** R6: suspend a member. */
    public void suspendMember(MemberId memberId) {
        requireMember(memberId).suspend();
    }

    /** R6: reinstate a member. */
    public void reinstateMember(MemberId memberId) {
        requireMember(memberId).reinstate();
    }

    /** R6 + R13: publish an offer. Rejected for a suspended member or an unknown category. */
    public Offer publishOffer(MemberId memberId, CategoryName category) {
        Member member = requireMember(memberId);
        requireActive(member, "publish an offer");
        catalogue.requireKnown(category);
        return new Offer(memberId, category);
    }

    /** R6 + R13 + R9: raise a request. Rejected for a suspended member or an unknown category. */
    public Request raiseRequest(MemberId memberId, CategoryName category, FulfilmentWindow window) {
        Member member = requireMember(memberId);
        requireActive(member, "raise a request");
        catalogue.requireKnown(category);
        return new Request(memberId, category, window);
    }

    /**
     * Match an offer to a request, honouring R5 (no self-fulfilment), R6 (no suspended party) and R9
     * (session date inside the request's window). The {@link Engagement} constructor enforces R5 and R9;
     * this method enforces R6 and the category match.
     */
    public Engagement proposeEngagement(Offer offer, Request request, Duration hours, LocalDate sessionDate) {
        if (!offer.category().equals(request.category())) {
            throw new IllegalArgumentException(
                    "offer category " + offer.category() + " does not match request category " + request.category());
        }
        requireActive(requireMember(offer.provider()), "propose an engagement");
        requireActive(requireMember(request.requester()), "propose an engagement");

        Engagement engagement = new Engagement(
                offer.provider(), request.requester(), offer.category(), hours, sessionDate, request.window());
        engagement.accept(); // the match is accepted immediately in this CLI model
        engagements.add(engagement);
        return engagement;
    }

    /**
     * R4 + R2 + R10: each party confirms independently; on the second confirmation the transfer happens,
     * with the levy, respecting the floor.
     */
    public CommandResult confirmSession(Engagement engagement, MemberId party) {
        Objects.requireNonNull(engagement, "engagement");
        Objects.requireNonNull(party, "party");

        // INV-4 pre-check: before the state transition, refuse a settlement that would breach the floor.
        if (engagement.isSecondConfirmation(party)) {
            Credits cost = engagement.hours().cost();
            int requesterBalance = ledger.balanceOf(engagement.requester());
            if (requesterBalance - cost.value() < LevyPolicy.OVERDRAFT_FLOOR) {
                return CommandResult.failure(
                        "confirming would leave the requester's balance below the −180 overdraft floor");
            }
        }

        boolean settled = engagement.confirm(party);
        if (!settled) {
            return CommandResult.ok("confirmation recorded; awaiting the other party");
        }

        // The transfer (INV-3: balanced) and the levy (INV-7: rounded down, capped, requester pays).
        Credits cost = engagement.hours().cost();
        int balanceAfterCost = ledger.balanceOf(engagement.requester()) - cost.value();
        Credits levy = LevyPolicy.levy(cost, balanceAfterCost);

        Account requester = new Account.MemberAccount(engagement.requester());
        Account provider = new Account.MemberAccount(engagement.provider());
        Account fund = new Account.FundAccount();

        ledger.append(EntryType.TRANSFER, requester, provider, cost, clock.now(), "session payment");
        ledger.append(EntryType.LEVY, requester, fund, levy, clock.now(), "community levy"); // even when 0

        return CommandResult.ok("session settled; " + cost.value() + " credits transferred, levy " + levy.value());
    }

    /** R14: cancel an engagement; a late cancellation writes a zero-amount record against the canceller. */
    public CommandResult cancelEngagement(Engagement engagement, MemberId party) {
        Objects.requireNonNull(engagement, "engagement");
        Objects.requireNonNull(party, "party");

        EngagementState resulting = engagement.cancel(party, clock.now());
        if (resulting == EngagementState.CANCELLED_LATE) {
            // INV-14: no TRANSFER/LEVY is written, but a zero-amount record names the cancelling member.
            ledger.append(EntryType.LATE_CANCELLATION,
                    new Account.MemberAccount(party),
                    new Account.FundAccount(),
                    Credits.ZERO,
                    clock.now(),
                    "late cancellation recorded against " + party);
        }
        return CommandResult.ok("engagement " + resulting.name().toLowerCase());
    }

    /** R11: apply for a community grant. Subject to the fund's solvency, the recipient being in deficit,
     * and at most one grant per calendar month. */
    public CommandResult applyForGrant(MemberId memberId, Credits amount) {
        requireMember(memberId);
        int fundBalance = ledger.fundBalance();
        int recipientBalance = ledger.balanceOf(memberId);
        boolean alreadyThisMonth = hasGrantThisMonth(memberId);

        CommunityFund.GrantDecision decision =
                CommunityFund.evaluate(amount, fundBalance, recipientBalance, alreadyThisMonth);
        if (!decision.approved()) {
            return CommandResult.failure(decision.reason());
        }

        ledger.append(EntryType.FUND_GRANT,
                new Account.FundAccount(),
                new Account.MemberAccount(memberId),
                amount,
                clock.now(),
                "community fund grant");
        return CommandResult.ok("grant of " + amount.value() + " credits issued to " + memberId);
    }

    // --- §5 read operations (R12: all read-only, no mutable reference escapes) ---

    public int queryBalance(MemberId memberId) {
        requireMember(memberId);
        return ledger.balanceOf(memberId);
    }

    public List<LedgerEntry> queryLedger() {
        return ledger.entries(); // immutable snapshot
    }

    public List<Engagement> queryEngagements(MemberId memberId) {
        requireMember(memberId);
        return engagements.stream()
                .filter(e -> e.provider().equals(memberId) || e.requester().equals(memberId))
                .toList();
    }

    /** R3 self-check: total credits in circulation and the fund balance. */
    public Report report() {
        int total = 0;
        for (Member member : members.values()) {
            total += ledger.balanceOf(member.id());
        }
        total += ledger.fundBalance();
        return new Report(total, ledger.fundBalance());
    }

    public int memberCount() {
        return members.size();
    }

    /** Every member's derived balance, keyed by member id. Exposed for the invariant test harness. */
    public Map<MemberId, Integer> allBalances() {
        Map<MemberId, Integer> result = new LinkedHashMap<>();
        for (Member member : members.values()) {
            result.put(member.id(), ledger.balanceOf(member.id()));
        }
        return result;
    }

    // --- helpers ---------------------------------------------------------

    private Member requireMember(MemberId memberId) {
        Member member = members.get(memberId);
        if (member == null) {
            throw new IllegalArgumentException("unknown member: " + memberId);
        }
        return member;
    }

    private void requireActive(Member member, String action) {
        if (member.isSuspended()) {
            throw new IllegalStateException("a suspended member may not " + action);
        }
    }

    private boolean hasGrantThisMonth(MemberId memberId) {
        YearMonth month = YearMonth.from(clock.today());
        Account memberAccount = new Account.MemberAccount(memberId);
        for (LedgerEntry e : ledger.entries()) {
            if (e.type() == EntryType.FUND_GRANT
                    && e.to().equals(memberAccount)
                    && YearMonth.from(e.recordedAt().atZone(java.time.ZoneOffset.UTC)).equals(month)) {
                return true;
            }
        }
        return false;
    }

    /** The report of total credits in circulation and the fund balance (R3 self-check). */
    public record Report(int totalCreditsInCirculation, int fundBalance) {
    }
}
