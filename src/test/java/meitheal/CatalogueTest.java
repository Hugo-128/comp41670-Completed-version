package meitheal;

import meitheal.application.MeithealService;
import meitheal.domain.catalogue.SkillCatalogue;
import meitheal.domain.model.CategoryName;
import meitheal.domain.model.FulfilmentWindow;
import meitheal.domain.model.MemberId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static meitheal.TestSupport.FURNITURE;
import static meitheal.TestSupport.email;
import static meitheal.TestSupport.newService;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins INV-13 (R13): every offer and request names a category in the startup catalogue; the catalogue's
 * contents are immutable at runtime.
 */
class CatalogueTest {

    private static final FulfilmentWindow WINDOW =
            new FulfilmentWindow(LocalDate.now().minusDays(1), LocalDate.now().plusDays(7));
    private static final CategoryName UNKNOWN = new CategoryName("Underwater basket weaving");

    @Test
    void unknownCategoryRejectedForOffer() {
        MeithealService service = newService();
        MemberId bob = service.registerMember("Bob", email("Bob"));
        assertThrows(IllegalArgumentException.class, () -> service.publishOffer(bob, UNKNOWN));
    }

    @Test
    void unknownCategoryRejectedForRequest() {
        MeithealService service = newService();
        MemberId alice = service.registerMember("Alice", email("Alice"));
        assertThrows(IllegalArgumentException.class, () -> service.raiseRequest(alice, UNKNOWN, WINDOW));
    }

    @Test
    void contentsCannotBeMutatedAtRuntime() {
        SkillCatalogue catalogue = new SkillCatalogue(List.of(FURNITURE));
        assertThrows(UnsupportedOperationException.class,
                () -> catalogue.categories().add(new CategoryName("Hacking")));
    }
}
