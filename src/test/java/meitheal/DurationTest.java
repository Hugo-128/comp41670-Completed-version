package meitheal;

import meitheal.domain.model.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins INV-6 (R8): a recorded session must be longer than zero and no longer than 8 hours.
 */
class DurationTest {

    @Test
    void zeroRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Duration(0));
    }

    @Test
    void negativeRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Duration(-1));
    }

    @Test
    void exactlyEightAccepted() {
        assertEquals(8, new Duration(8).hours());
    }

    @Test
    void justOverEightRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Duration(9));
    }
}
