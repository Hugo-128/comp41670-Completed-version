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

import static meitheal.TestSupport.FURNITURE;
import static meitheal.TestSupport.email;
import static meitheal.TestSupport.newService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins INV-8 (R4): credits move only after two <em>distinct</em> parties confirm; one party confirming
 * twice does not settle; a settled engagement settles exactly once.
 */
class ConfirmationTest {

    private static final FulfilmentWindow WINDOW =
            new FulfilmentWindow(LocalDate.now().minusDays(1), LocalDate.now().plusDays(7));

    private Engagement engage(MeithealService service, MemberId requester, MemberId provider, int hours) {
        Offer offer = service.publishOffer(provider, FURNITURE);
        Request request = service.raiseRequest(requester, FURNITURE, WINDOW);
        return service.proposeEngagement(offer, request, new Duration(hours), LocalDate.now());
    }

    @Test
    void singleConfirmationMovesNoCredits() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));

        Engagement e = engage(service, alice, bob, 2);
        service.confirmSession(e, alice); // only the requester confirms

        assertFalse(e.isSettled(), "one confirmation is not enough");
        assertEquals(120, service.queryBalance(bob), "no credits move on a single confirmation");
    }

    @Test
    void samePartyConfirmingTwiceDoesNotSettle() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));

        Engagement e = engage(service, alice, bob, 2);
        service.confirmSession(e, alice);
        assertThrows(IllegalStateException.class, () -> service.confirmSession(e, alice),
                "the same party cannot confirm twice");
        assertFalse(e.isSettled());
    }

    @Test
    void secondDistinctConfirmationSettlesExactlyOnce() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));

        Engagement e = engage(service, alice, bob, 2);
        service.confirmSession(e, alice);
        service.confirmSession(e, bob);

        assertTrue(e.isSettled());
        assertEquals(120 + 120, service.queryBalance(bob), "provider paid 2 × 60");
    }

    @Test
    void thirdConfirmationDoesNotDoublePay() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));

        Engagement e = engage(service, alice, bob, 2);
        service.confirmSession(e, alice);
        service.confirmSession(e, bob);
        int bobAfterSettle = service.queryBalance(bob);

        service.confirmSession(e, bob); // a third confirmation is a no-op
        assertEquals(bobAfterSettle, service.queryBalance(bob), "no second transfer");
    }
}
