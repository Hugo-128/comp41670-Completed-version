package meitheal.domain.request;

import meitheal.domain.model.CategoryName;
import meitheal.domain.model.FulfilmentWindow;
import meitheal.domain.model.MemberId;

import java.util.Objects;

/**
 * A request for help in a known category, carrying a fulfilment window (R9).
 *
 * <p>Immutable. Category validity (R13 / INV-13) is checked against the {@code SkillCatalogue} at creation
 * time in {@code MeithealService}; this type only records what was asked for.
 */
public record Request(MemberId requester, CategoryName category, FulfilmentWindow window) {

    public Request {
        Objects.requireNonNull(requester, "requester");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(window, "window");
    }
}
