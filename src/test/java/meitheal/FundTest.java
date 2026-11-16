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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins INV-12 (R11): the fund grants only to a member in deficit, only what it holds, at most once per
 * calendar month.
 */
class FundTest {

    private static final FulfilmentWindow WINDOW =
            new FulfilmentWindow(LocalDate.now().minusDays(1), LocalDate.now().plusDays(7));

    @Test
    void rejectedWhenRecipientBalanceNonNegative() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice")); // balance 120, not in deficit

        CommandResult result = service.applyForGrant(alice, new Credits(5));
        assertFalse(result.success(), "a solvent member must not receive a grant");
    }

    @Test
    void rejectedWhenFundCannotCoverTheAmount() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));

        // Drive Alice into deficit; the fund holds only the 9-credit levy from one 3-hour session.
        Offer offer = service.publishOffer(bob, FURNITURE);
        Request request = service.raiseRequest(alice, FURNITURE, WINDOW);
        Engagement e = service.proposeEngagement(offer, request, new Duration(3), LocalDate.now());
        service.confirmSession(e, alice);
        service.confirmSession(e, bob);

        assertTrue(service.queryBalance(alice) < 0);
        CommandResult result = service.applyForGrant(alice, new Credits(50)); // fund has only 9
        assertFalse(result.success(), "the fund cannot grant credits it does not hold");
    }

    @Test
    void secondGrantInSameCalendarMonthRejected() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        MemberId bob = service.registerMember("Bob", email("Bob"));

        Offer offer = service.publishOffer(bob, FURNITURE);
        Request request = service.raiseRequest(alice, FURNITURE, WINDOW);
        Engagement e = service.proposeEngagement(offer, request, new Duration(3), LocalDate.now());
        service.confirmSession(e, alice);
        service.confirmSession(e, bob);

        CommandResult first = service.applyForGrant(alice, new Credits(5)); // fits within the 9-credit fund
        assertTrue(first.success(), first.message());

        CommandResult second = service.applyForGrant(alice, new Credits(2));
        assertFalse(second.success(), "at most one grant per calendar month");
    }
}
