package meitheal.domain.model;

/**
 * The identity of a member. A value object wrapping a stable string identifier, so that a member's
 * identity cannot be accidentally swapped for an email address, a name, or some other string.
 */
public record MemberId(String value) {

    public MemberId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("member id must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
