package meitheal;

import meitheal.application.MeithealService;
import meitheal.domain.engagement.Engagement;
import meitheal.domain.model.Credits;
import meitheal.domain.model.Duration;
import meitheal.domain.model.FulfilmentWindow;
import meitheal.domain.model.MemberId;
import meitheal.domain.policy.LevyPolicy;
import meitheal.domain.request.Offer;
import meitheal.domain.request.Request;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static meitheal.TestSupport.FURNITURE;
import static meitheal.TestSupport.email;
import static meitheal.TestSupport.newService;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins INV-7 (R10): the levy is 5% rounded <em>down</em>, paid by the requester, capped at the available
 * headroom so it never itself breaches the floor. The rounding boundary (19 → 0, 20 → 1) is where a
 * wrong implementation (Math.round) would fail.
 */
class LevyTest {

    @Test
    void roundsDownNotToNearest() {
        assertEquals(0, LevyPolicy.fullLevy(new Credits(19)).value(), "⌊0.95⌋ = 0");
        assertEquals(1, LevyPolicy.fullLevy(new Credits(20)).value(), "⌊1.00⌋ = 1");
        assertEquals(3, LevyPolicy.fullLevy(new Credits(60)).value(), "⌊3.00⌋ = 3");
    }

    @Test
    void cappedAtHeadroomWhenFullLevyWouldBreachFloor() {
        assertEquals(0, LevyPolicy.levy(new Credits(60), -180).value(), "no headroom → zero levy");
        assertEquals(1, LevyPolicy.levy(new Credits(60), -179).value(), "headroom 1 → capped to 1");
        assertEquals(3, LevyPolicy.levy(new Credits(60), -175).value(), "headroom 5 → full 3");
    }

    @Test
    void paidByRequesterNotProvider() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice")); // requester, 120
        MemberId bob = service.registerMember("Bob", email("Bob"));       // provider, 120

        FulfilmentWindow window =
                new FulfilmentWindow(LocalDate.now().minusDays(1), LocalDate.now().plusDays(7));
        Offer offer = service.publishOffer(bob, FURNITURE);
        Request request = service.raiseRequest(alice, FURNITURE, window);
        Engagement e = service.proposeEngagement(offer, request, new Duration(2), LocalDate.now());

        service.confirmSession(e, alice);
        service.confirmSession(e, bob);

        // cost 120, levy ⌊120×0.05⌋ = 6. Provider receives exactly cost; requester pays cost + levy.
        assertEquals(120 + 120, service.queryBalance(bob), "provider receives exactly the session cost");
        assertEquals(120 - 120 - 6, service.queryBalance(alice), "requester pays cost plus the levy");
        assertEquals(6, service.report().fundBalance(), "the levy lands in the fund");
    }
}
