package meitheal.domain.catalogue;

import meitheal.domain.model.CategoryName;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * The fixed set of skill categories (R13).
 *
 * <p>INV-13: categories are fixed when the system starts and loaded from a configuration source; a request
 * or offer naming a category that does not exist is rejected. The catalogue is immutable ({@code Set.copyOf}),
 * loaded once at bootstrap, so its contents are identical in every reachable state.
 *
 * <p>R12 is also discharged here: {@link #categories()} returns the already-immutable set, so no caller can
 * mutate it.
 */
public final class SkillCatalogue {

    private final Set<CategoryName> categories;

    public SkillCatalogue(Collection<CategoryName> categories) {
        Objects.requireNonNull(categories, "categories");
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("the catalogue must contain at least one category");
        }
        this.categories = Set.copyOf(categories);
    }

    /** True when {@code name} is a known category. */
    public boolean contains(CategoryName name) {
        return categories.contains(name);
    }

    /** Rejects an unknown category — the single gate every Offer and Request must pass (INV-13). */
    public void requireKnown(CategoryName name) {
        if (!contains(name)) {
            throw new IllegalArgumentException("unknown skill category: " + name);
        }
    }

    /** An immutable view of the categories (already unmodifiable via {@code Set.copyOf}). */
    public Set<CategoryName> categories() {
        return categories;
    }
}
