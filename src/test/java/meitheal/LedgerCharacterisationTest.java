package meitheal;

import meitheal.domain.ledger.Account;
import meitheal.domain.ledger.EntryType;
import meitheal.domain.ledger.Ledger;
import meitheal.domain.ledger.LedgerEntry;
import meitheal.domain.model.Credits;
import meitheal.domain.model.MemberId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Characterisation tests for Ledger (Lab 9 hotspot).
// These pin what the code does NOW, not what spec says. Green before refactor, green after.
class LedgerCharacterisationTest {

    private static final Instant T = Instant.parse("2026-11-15T10:00:00Z");
    private static final MemberId ALICE = new MemberId("M1");
    private static final MemberId BOB = new MemberId("M2");

    private static final Account ALICE_ACCOUNT = new Account.MemberAccount(ALICE);
    private static final Account BOB_ACCOUNT = new Account.MemberAccount(BOB);
    private static final Account FUND = new Account.FundAccount();
    private static final Account SYSTEM = new Account.SystemAccount();

    private static Ledger ledgerWith(EntryType type, Account from, Account to, int amount) {
        Ledger ledger = new Ledger();
        ledger.append(type, from, to, new Credits(amount), T, "characterisation fixture");
        return ledger;
    }

    // INV-1 append-only

    @Test
    void appendReturnsTheWrittenEntryInSequenceOrder() {
        Ledger ledger = new Ledger();
        LedgerEntry first = ledger.append(EntryType.WELCOME_GRANT, SYSTEM, ALICE_ACCOUNT,
                new Credits(120), T, "welcome");
        LedgerEntry second = ledger.append(EntryType.TRANSFER, ALICE_ACCOUNT, BOB_ACCOUNT,
                new Credits(60), T, "payment");

        assertEquals(0, first.sequence());
        assertEquals(1, second.sequence());
        assertEquals(2, ledger.size());
    }

    @Test
    void entriesSnapshotIsImmutableAndReflectsAllAppends() {
        Ledger ledger = ledgerWith(EntryType.WELCOME_GRANT, SYSTEM, ALICE_ACCOUNT, 120);

        List<LedgerEntry> snapshot = ledger.entries();
        assertEquals(1, snapshot.size());

        // The snapshot must not mutate the backing list, nor the backing list mutate the snapshot.
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(null));

        ledger.append(EntryType.LEVY, ALICE_ACCOUNT, FUND, new Credits(3), T, "levy");
        assertEquals(1, snapshot.size(), "snapshot must be a copy, not a live view");
        assertEquals(2, ledger.size());
    }

    // INV-2 balance derived, not stored

    @Test
    void balanceOfSumsCreditsInAndDebitsOut() {
        Ledger ledger = new Ledger();
        ledger.append(EntryType.WELCOME_GRANT, SYSTEM, ALICE_ACCOUNT, new Credits(120), T, "welcome");
        ledger.append(EntryType.TRANSFER, ALICE_ACCOUNT, BOB_ACCOUNT, new Credits(60), T, "payment");

        // Alice: +120 then −60 = 60. Bob: +60. Fund: 0. System: −120.
        assertEquals(60, ledger.balanceOf(ALICE));
        assertEquals(60, ledger.balanceOf(BOB));
        assertEquals(0, ledger.fundBalance());
        assertEquals(-120, ledger.balanceOf(SYSTEM));
    }

    @Test
    void balanceOfAccountVariantMatchesMemberVariant() {
        Ledger ledger = ledgerWith(EntryType.WELCOME_GRANT, SYSTEM, ALICE_ACCOUNT, 120);

        // The two overloads must agree: balanceOf(MemberId) delegates to balanceOf(Account).
        assertEquals(ledger.balanceOf(ALICE_ACCOUNT), ledger.balanceOf(ALICE));
    }

    @Test
    void balanceCanGoNegative() {
        Ledger ledger = new Ledger();
        ledger.append(EntryType.TRANSFER, ALICE_ACCOUNT, BOB_ACCOUNT, new Credits(200), T, "large");
        assertTrue(ledger.balanceOf(ALICE) < 0, "a member may be driven into deficit");
    }

    // fund balance

    @Test
    void fundBalanceIsNetLevyInMinusGrantsOut() {
        Ledger ledger = new Ledger();
        ledger.append(EntryType.LEVY, ALICE_ACCOUNT, FUND, new Credits(9), T, "levy");
        ledger.append(EntryType.FUND_GRANT, FUND, BOB_ACCOUNT, new Credits(5), T, "grant");
        assertEquals(4, ledger.fundBalance());
    }

    // welcomeGrantCount

    @Test
    void welcomeGrantCountCountsOnlyWelcomeGrants() {
        Ledger ledger = new Ledger();
        ledger.append(EntryType.WELCOME_GRANT, SYSTEM, ALICE_ACCOUNT, new Credits(120), T, "welcome");
        ledger.append(EntryType.TRANSFER, ALICE_ACCOUNT, BOB_ACCOUNT, new Credits(60), T, "payment");
        ledger.append(EntryType.WELCOME_GRANT, SYSTEM, BOB_ACCOUNT, new Credits(120), T, "welcome");

        assertEquals(2, ledger.welcomeGrantCount());
    }

    // arg validation (currently redundant, see debt register)

    @Test
    void appendRejectsNullArguments() {
        Ledger ledger = new Ledger();
        assertThrows(NullPointerException.class,
                () -> ledger.append(null, SYSTEM, ALICE_ACCOUNT, new Credits(1), T, "x"));
        assertThrows(NullPointerException.class,
                () -> ledger.append(EntryType.TRANSFER, null, ALICE_ACCOUNT, new Credits(1), T, "x"));
        assertThrows(NullPointerException.class,
                () -> ledger.append(EntryType.TRANSFER, SYSTEM, null, new Credits(1), T, "x"));
        assertThrows(NullPointerException.class,
                () -> ledger.append(EntryType.TRANSFER, SYSTEM, ALICE_ACCOUNT, null, T, "x"));
    }

    @Test
    void entryRejectsSelfTransfer() {
        Ledger ledger = new Ledger();
        // The LedgerEntry compact constructor enforces from != to, surfaced through append.
        assertThrows(IllegalArgumentException.class,
                () -> ledger.append(EntryType.TRANSFER, ALICE_ACCOUNT, ALICE_ACCOUNT,
                        new Credits(1), T, "self"));
    }

    @Test
    void lateCancellationEntryCarriesZeroAmountAndIsStillCounted() {
        Ledger ledger = new Ledger();
        LedgerEntry entry = ledger.append(EntryType.LATE_CANCELLATION, ALICE_ACCOUNT, FUND,
                Credits.ZERO, T, "late");

        assertEquals(0, entry.amount().value());
        assertEquals(1, ledger.size());
        assertFalse(ledger.entries().isEmpty());
    }

    // adversarial inversion: what if from == to? LedgerEntry rejects it at construction,
    // so effectOnAccount's early-return (to branch first) can never be reached with from==to.
    @Test
    void selfTransferIsRejectedSoEffectOnAccountEarlyReturnIsSafe() {
        Ledger ledger = new Ledger();
        assertThrows(IllegalArgumentException.class,
                () -> ledger.append(EntryType.TRANSFER, ALICE_ACCOUNT, ALICE_ACCOUNT,
                        new Credits(5), T, "self"));
    }
}
