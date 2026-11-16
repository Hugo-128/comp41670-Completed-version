package meitheal;

import meitheal.domain.model.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins INV-5 (R1): an hour is an hour — the cost is 60 credits per hour, independent of the skill
 * category, and there is no code path that accepts a caller-supplied amount.
 */
class PricingTest {

    @Test
    void costIsSixtyTimesHours() {
        assertEquals(60, new Duration(1).cost().value());
        assertEquals(120, new Duration(2).cost().value());
        assertEquals(480, new Duration(8).cost().value());
    }

    @Test
    void legalAdviceAndFurnitureMovingCostTheSame() {
        // R1: no premium skill, no discounted skill. The cost depends only on hours.
        // Duration.cost() has no category parameter, so the only way to price two sessions
        // differently is to give them different hours — which is exactly the point being pinned.
        Duration legal = new Duration(3);
        Duration moving = new Duration(3);
        assertEquals(legal.cost(), moving.cost());
    }
}
