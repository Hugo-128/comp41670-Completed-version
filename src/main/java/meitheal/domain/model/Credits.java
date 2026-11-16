package meitheal.domain.model;

import java.util.Objects;

/**
 * A non-negative quantity of time credits.
 *
 * <p>Credits are the unit of account in Meitheal. They are <em>created</em> only in the two ways R3 allows
 * (the welcome grant, and community-fund grants); every other movement is a zero-sum transfer. Because a
 * single credit amount can never be negative, the type enforces that at construction, so an amount that
 * does not exist (a negative "quantity of credits") cannot be represented. Member <em>balances</em>, which
 * may go as low as −180, are derived from the ledger as plain {@code int} values and deliberately do not
 * use this type.
 *
 * <p>Enforces: nothing by itself — it is the building block that INV-3 (conservation) and INV-4 (the
 * −180 floor) are expressed in terms of.
 */
public record Credits(int value) {

    public Credits {
        if (value < 0) {
            throw new IllegalArgumentException("credits must be non-negative, got " + value);
        }
    }

    public static final Credits ZERO = new Credits(0);

    /** Adds two non-negative amounts. The sum is never negative, so a {@code Credits} is still valid. */
    public Credits plus(Credits other) {
        Objects.requireNonNull(other, "other");
        return new Credits(this.value + other.value);
    }

    /** Subtracts {@code other} from this, as an integer. Returns a negative value when {@code other > this}. */
    public int minus(Credits other) {
        Objects.requireNonNull(other, "other");
        return this.value - other.value;
    }

    /** True when this amount is greater than {@code other}. */
    public boolean greaterThan(Credits other) {
        Objects.requireNonNull(other, "other");
        return this.value > other.value;
    }
}
