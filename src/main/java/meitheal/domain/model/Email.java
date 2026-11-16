package meitheal.domain.model;

/**
 * A member's email address, used at registration (R3 / §5 "Register a member": create a member with a name
 * and email). A minimal value object that rejects blank input; it deliberately does not try to validate the
 * full RFC 5322 grammar — an over-strict regex would be a policy invented by us, not by the brief.
 */
public record Email(String value) {

    public Email {
        if (value == null || value.isBlank() || !value.contains("@")) {
            throw new IllegalArgumentException("email must be non-blank and contain '@', got: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
