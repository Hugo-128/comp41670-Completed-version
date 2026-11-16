package meitheal;

import meitheal.application.CommandResult;
import meitheal.application.MeithealService;
import meitheal.domain.engagement.Engagement;
import meitheal.domain.model.Credits;
import meitheal.domain.model.Duration;
import meitheal.domain.model.FulfilmentWindow;
import meitheal.domain.model.MemberId;
import meitheal.domain.request.Offer;
import meitheal.domain.request.Request;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static meitheal.TestSupport.FURNITURE;
import static meitheal.TestSupport.email;
import static meitheal.TestSupport.newService;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins INV-10 (R6) and our resolution of the R4↔R6 tension (decision D-a): suspension forbids the
 * <em>creation</em> of new commitments, but never the <em>completion</em> of a commitment that already
 * existed. The last test is the guard: if a future contributor "helpfully" adds a status check to
 * {@code confirmSession}, this test goes red.
 */
class SuspensionTest {

    private static final FulfilmentWindow WINDOW =
            new FulfilmentWindow(LocalDate.now().minusDays(1), LocalDate.now().plusDays(7));

    @Test
    void cannotRaiseRequestWhileSuspended() {
        MeithealService service = newService();
        MemberId bob = service.registerMember("Bob", email("Bob"));
        service.suspendMember(bob);
        assertThrows(IllegalStateException.class, () -> service.raiseRequest(bob, FURNITURE, WINDOW));
    }

    @Test
    void cannotPublishOfferWhileSuspended() {
        MeithealService service = newService();
        MemberId bob = service.registerMember("Bob", email("Bob"));
        service.suspendMember(bob);
        assertThrows(IllegalStateException.class, () -> service.publishOffer(bob, FURNITURE));
    }

    @Test
    void cannotEnterNewEngagementWhileSuspended() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));
        Offer offer = service.publishOffer(bob, FURNITURE);
        Request request = service.raiseRequest(alice, FURNITURE, WINDOW);

        service.suspendMember(bob);
        assertThrows(IllegalStateException.class,
                () -> service.proposeEngagement(offer, request, new Duration(1), LocalDate.now()));
    }

    @Test
    void canStillConfirmPreSuspensionEngagement() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));

        Offer offer = service.publishOffer(bob, FURNITURE);
        Request request = service.raiseRequest(alice, FURNITURE, WINDOW);
        Engagement engagement = service.proposeEngagement(offer, request, new Duration(2), LocalDate.now());

        // The requester confirms first, then the provider is suspended before they confirm.
        service.confirmSession(engagement, alice);
        service.suspendMember(bob);

        // D-a: the provider must still be able to complete work that was already in flight.
        CommandResult result = service.confirmSession(engagement, bob);
        assertTrue(result.success(), result.message());
        assertTrue(engagement.isSettled());
    }

    @Test
    void suspendedMemberStillReceivesCredits() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));

        Offer offer = service.publishOffer(bob, FURNITURE);
        Request request = service.raiseRequest(alice, FURNITURE, WINDOW);
        Engagement engagement = service.proposeEngagement(offer, request, new Duration(3), LocalDate.now());

        int bobBefore = service.queryBalance(bob);
        service.confirmSession(engagement, alice);
        service.suspendMember(bob);
        service.confirmSession(engagement, bob); // completes after suspension

        // Bob was credited 3 × 60 = 180 credits for the completed session.
        assertTrue(service.queryBalance(bob) > bobBefore);
        assertTrue(service.queryBalance(bob) == bobBefore + 180);
    }

    @Test
    void grantToMemberInDeficitStillWorked() {
        // A member driven into deficit by confirming sessions can still receive a community grant (R11),
        // which is a second way credits enter the system — the conservation test covers the total.
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));

        Offer offer = service.publishOffer(bob, FURNITURE);
        Request request = service.raiseRequest(alice, FURNITURE, WINDOW);
        Engagement engagement = service.proposeEngagement(offer, request, new Duration(3), LocalDate.now());
        service.confirmSession(engagement, alice);
        service.confirmSession(engagement, bob);

        // Alice paid 180 + levy, so she is now in deficit (120 − 189 = −69).
        assertTrue(service.queryBalance(alice) < 0);
        // The fund holds only the 9-credit levy, so a grant must fit within that.
        CommandResult grant = service.applyForGrant(alice, new Credits(5));
        assertTrue(grant.success(), grant.message());
    }
}
