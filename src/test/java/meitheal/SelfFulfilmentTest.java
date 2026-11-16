package meitheal;

import meitheal.application.MeithealService;
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

/**
 * Pins INV-9 (R5): a member may not fulfil their own request.
 */
class SelfFulfilmentTest {

    private static final FulfilmentWindow WINDOW =
            new FulfilmentWindow(LocalDate.now().minusDays(1), LocalDate.now().plusDays(7));

    @Test
    void memberCannotMatchOwnRequest() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));

        Offer offer = service.publishOffer(alice, FURNITURE);   // Alice offers
        Request request = service.raiseRequest(alice, FURNITURE, WINDOW); // Alice requests

        assertThrows(IllegalArgumentException.class,
                () -> service.proposeEngagement(offer, request, new Duration(1), LocalDate.now()));
    }
}
