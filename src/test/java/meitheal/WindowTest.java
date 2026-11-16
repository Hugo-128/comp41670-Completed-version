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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins INV-11 (R9): an engagement may be confirmed only for a date inside the request's fulfilment
 * window [earliest, latest].
 */
class WindowTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static final FulfilmentWindow WINDOW =
            new FulfilmentWindow(TODAY, TODAY.plusDays(7));

    private MeithealService service;
    private MemberId alice;
    private MemberId bob;

    private void setup() {
        service = newService();
        alice = service.registerMember("Alice", email("Alice"));
        bob = service.registerMember("Bob", email("Bob"));
    }

    private void proposeOn(LocalDate sessionDate) {
        Offer offer = service.publishOffer(bob, FURNITURE);
        Request request = service.raiseRequest(alice, FURNITURE, WINDOW);
        service.proposeEngagement(offer, request, new Duration(2), sessionDate);
    }

    @Test
    void earliestDateAccepted() {
        setup();
        proposeOn(WINDOW.earliest());
        assertTrue(true, "proposing on the earliest date must not throw");
    }

    @Test
    void latestDateAccepted() {
        setup();
        proposeOn(WINDOW.latest());
        assertTrue(true);
    }

    @Test
    void dayBeforeEarliestRejected() {
        setup();
        assertThrows(IllegalArgumentException.class, () -> proposeOn(WINDOW.earliest().minusDays(1)));
    }

    @Test
    void dayAfterLatestRejected() {
        setup();
        assertThrows(IllegalArgumentException.class, () -> proposeOn(WINDOW.latest().plusDays(1)));
    }
}
