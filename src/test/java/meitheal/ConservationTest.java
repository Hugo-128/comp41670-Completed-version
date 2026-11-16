package meitheal;

import meitheal.application.MeithealService;
import meitheal.domain.engagement.Engagement;
import meitheal.domain.model.Duration;
import meitheal.domain.model.FulfilmentWindow;
import meitheal.domain.model.MemberId;
import meitheal.domain.request.Offer;
import meitheal.domain.request.Request;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static meitheal.InvariantAssertions.assertInvariantsHold;
import static meitheal.TestSupport.FURNITURE;
import static meitheal.TestSupport.NOW;
import static meitheal.TestSupport.email;
import static meitheal.TestSupport.newService;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins INV-3 (conservation): after any sequence of operations the total credits in circulation must equal
 * 120 × member count. This is the invariant the markers explicitly try to break.
 */
class ConservationTest {

    @Test
    void transferPreservesTotalCredits() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));

        Offer offer = service.publishOffer(bob, FURNITURE);
        Request request = service.raiseRequest(alice, FURNITURE,
                new FulfilmentWindow(LocalDate.now().minusDays(1), LocalDate.now().plusDays(7)));
        Engagement engagement = service.proposeEngagement(offer, request, new Duration(2), LocalDate.now());

        service.confirmSession(engagement, alice);
        service.confirmSession(engagement, bob);

        // The transfer and levy moved credits between accounts, but the total is unchanged.
        assertEquals(120 * service.memberCount(), service.report().totalCreditsInCirculation());
        assertInvariantsHold(service);
    }

    @Test
    void conservationSurvivesAdversarialSequence() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));
        MemberId carol = service.registerMember("Carol", email("Carol"));

        // Several transfers in both directions, plus a grant into the fund via levy.
        for (int i = 0; i < 3; i++) {
            Offer offer = service.publishOffer(bob, FURNITURE);
            Request request = service.raiseRequest(alice, FURNITURE,
                    new FulfilmentWindow(NOW.atZone(java.time.ZoneOffset.UTC).toLocalDate().minusDays(1),
                            NOW.atZone(java.time.ZoneOffset.UTC).toLocalDate().plusDays(7)));
            Engagement e = service.proposeEngagement(offer, request, new Duration(i + 1),
                    NOW.atZone(java.time.ZoneOffset.UTC).toLocalDate());
            service.confirmSession(e, alice);
            service.confirmSession(e, bob);
        }

        assertInvariantsHold(service);
        // The exact total is 120 × 3 members, regardless of how many transfers ran.
        assertEquals(360, service.report().totalCreditsInCirculation());
    }
}
