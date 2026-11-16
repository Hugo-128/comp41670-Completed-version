package meitheal.domain.request;

import meitheal.domain.model.CategoryName;
import meitheal.domain.model.MemberId;

import java.util.Objects;

/**
 * An offer to provide help in a known category.
 *
 * <p>Immutable. As with {@link Request}, category validity (R13 / INV-13) is checked at creation time in
 * {@code MeithealService}.
 */
public record Offer(MemberId provider, CategoryName category) {

    public Offer {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(category, "category");
    }
}
