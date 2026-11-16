package meitheal.domain.model;

/**
 * The name of a skill category.
 *
 * <p>R13: categories are fixed when the system starts and loaded from a configuration source; a request or
 * offer naming an unknown category is rejected. The name itself is a value object, but the validity check
 * (is this name in the fixed catalogue?) belongs to {@code SkillCatalogue} — this type only guards that a
 * name is a non-blank string.
 */
public record CategoryName(String value) {

    public CategoryName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("category name must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
