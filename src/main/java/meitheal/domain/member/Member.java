package meitheal.domain.member;

import meitheal.domain.model.Email;
import meitheal.domain.model.MemberId;

import java.util.Objects;

/**
 * A member of the time-bank. The aggregate root for member identity and lifecycle status.
 *
 * <p>A member deliberately does <em>not</em> hold a balance. INV-2: a member's balance is the net of their
 * ledger entries plus the welcome grant — there is no balance independent of the ledger. Storing a parallel
 * balance here would create a second source of truth that can drift from the ledger, which is exactly the
 * bug the conservation invariant (INV-3) exists to prevent.
 *
 * <p>Enforces INV-10 (suspension) at the {@link #suspend()} / {@link #reinstate()} boundary, but the actual
 * gate that stops a suspended member creating offers/requests/engagements lives in {@code MeithealService},
 * on the three creation methods only — see decision D-a.
 */
public final class Member {

    private final MemberId id;
    private final String name;
    private final Email email;
    private MemberStatus status;

    public Member(MemberId id, String name, Email email) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireNonBlank(name, "name");
        this.email = Objects.requireNonNull(email, "email");
        this.status = MemberStatus.ACTIVE;
    }

    /** R6: suspend a member. They keep the ability to receive credits for prior completed work. */
    public void suspend() {
        this.status = MemberStatus.SUSPENDED;
    }

    /** R6: reinstate a suspended member. */
    public void reinstate() {
        this.status = MemberStatus.ACTIVE;
    }

    public boolean isSuspended() {
        return status == MemberStatus.SUSPENDED;
    }

    /** The gate used by the three creation methods in {@code MeithealService}. */
    public MemberStatus status() {
        return status;
    }

    public MemberId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Email email() {
        return email;
    }

    private static String requireNonBlank(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }
}
