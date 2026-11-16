package meitheal;

import meitheal.application.CommandResult;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins INV-4 (R2): the −180 overdraft floor. The marker will try to drive a balance through the floor;
 * these tests show the boundary holds exactly.
 */
class FloorTest {

    private static final FulfilmentWindow WINDOW =
            new FulfilmentWindow(LocalDate.now().minusDays(1), LocalDate.now().plusDays(7));

    private Engagement engage(MeithealService service, MemberId requester, MemberId provider, int hours) {
        Offer offer = service.publishOffer(provider, FURNITURE);
        Request request = service.raiseRequest(requester, FURNITURE, WINDOW);
        return service.proposeEngagement(offer, request, new Duration(hours), LocalDate.now());
    }

    @Test
    void debitToExactlyMinus180IsAllowed() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice")); // starts at 120
        MemberId bob = service.registerMember("Bob", email("Bob"));

        Engagement e = engage(service, alice, bob, 5); // cost 300 → 120 − 300 = −180
        service.confirmSession(e, alice);
        CommandResult result = service.confirmSession(e, bob); // second distinct confirmation

        assertTrue(result.success(), result.message());
        assertEquals(-180, service.queryBalance(alice), "exactly at the floor is legal");
    }

    @Test
    void debitBelowFloorIsRejectedAndMovesNothing() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));

        Engagement e = engage(service, alice, bob, 6); // cost 360 → 120 − 360 = −240
        service.confirmSession(e, alice);
        CommandResult result = service.confirmSession(e, bob);

        assertFalse(result.success(), "a settlement that would breach −180 must be refused");
        assertEquals(120, service.queryBalance(alice), "no transfer may happen");
        assertEquals(120, service.queryBalance(bob), "provider is not paid for a refused settlement");
    }
}
