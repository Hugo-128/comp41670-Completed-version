package meitheal;

import meitheal.application.CommandResult;
import meitheal.application.MeithealService;
import meitheal.domain.engagement.Engagement;
import meitheal.domain.ledger.EntryType;
import meitheal.domain.ledger.LedgerEntry;
import meitheal.domain.model.Duration;
import meitheal.domain.model.FulfilmentWindow;
import meitheal.domain.model.MemberId;
import meitheal.domain.policy.CancellationPolicy;
import meitheal.domain.request.Offer;
import meitheal.domain.request.Request;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static meitheal.TestSupport.FURNITURE;
import static meitheal.TestSupport.email;
import static meitheal.TestSupport.newService;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins INV-14 (R14): cancelling less than 24 hours before the agreed start moves no credits but records
 * the cancelling member.
 */
class CancellationTest {

    private static final LocalDate SESSION = LocalDate.of(2026, 11, 16);

    @Test
    void cancelAtExactly24hIsNotLate() {
        Instant now = Instant.parse("2026-11-15T00:00:00Z");
        assertFalse(CancellationPolicy.isLateCancellation(SESSION, now));
    }

    @Test
    void cancelAt23h59mIsLate() {
        Instant now = Instant.parse("2026-11-15T00:01:00Z");
        assertTrue(CancellationPolicy.isLateCancellation(SESSION, now));
    }

    @Test
    void lateCancelMovesNoCreditsButIsRecorded() {
        MeithealService service = newService(); // clock = 2026-11-15T10:00Z, 14h before SESSION
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));

        FulfilmentWindow window =
                new FulfilmentWindow(SESSION.minusDays(1), SESSION.plusDays(1));
        Offer offer = service.publishOffer(bob, FURNITURE);
        Request request = service.raiseRequest(alice, FURNITURE, window);
        Engagement e = service.proposeEngagement(offer, request, new Duration(2), SESSION);

        CommandResult result = service.cancelEngagement(e, alice);
        assertTrue(result.success());

        boolean hasTransfer = service.queryLedger().stream()
                .anyMatch(le -> le.type() == EntryType.TRANSFER);
        boolean hasLateRecord = service.queryLedger().stream()
                .anyMatch(le -> le.type() == EntryType.LATE_CANCELLATION);
        assertFalse(hasTransfer, "a late cancellation moves no credits");
        assertTrue(hasLateRecord, "a late cancellation is recorded against the cancelling member");
    }
}
