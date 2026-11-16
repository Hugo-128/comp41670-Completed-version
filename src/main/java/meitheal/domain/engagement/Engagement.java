package meitheal.domain.engagement;

import meitheal.domain.model.CategoryName;
import meitheal.domain.model.Duration;
import meitheal.domain.model.FulfilmentWindow;
import meitheal.domain.model.MemberId;
import meitheal.domain.policy.CancellationPolicy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A matched offer-and-request: one provider will perform one session of work for one requester.
 *
 * <p>The aggregate root for the settlement workflow. It enforces three invariants directly:
 * <ul>
 *   <li><b>INV-9</b> (R5): provider ≠ requester — rejected in the constructor.</li>
 *   <li><b>INV-11</b> (R9): the session date must fall inside the request's fulfilment window — rejected
 *       in the constructor.</li>
 *   <li><b>INV-8</b> (R4): settlement requires two <em>distinct</em> confirmations; one party confirming
 *       twice does not settle, and a settled engagement settles exactly once.</li>
 *   <li><b>INV-14</b> (R14): cancellation less than 24h before the agreed start reaches a terminal
 *       {@code CANCELLED_LATE} state from which settlement is unreachable.</li>
 * </ul>
 *
 * <p>Note: the engagement holds no balance and writes no ledger entry itself. When the second distinct
 * confirmation arrives, {@link #confirm(MemberId)} returns {@code true}, and {@code MeithealService} is
 * the sole writer of the resulting TRANSFER and LEVY entries. Keeping the write out of the aggregate is
 * what keeps the conservation argument (INV-3) auditable from one place.
 */
public final class Engagement {

    private final MemberId provider;
    private final MemberId requester;
    private final CategoryName category;
    private final Duration hours;
    private final LocalDate sessionDate;
    private EngagementState state;
    private boolean providerConfirmed;
    private boolean requesterConfirmed;

    public Engagement(MemberId provider, MemberId requester, CategoryName category,
                      Duration hours, LocalDate sessionDate, FulfilmentWindow window) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.requester = Objects.requireNonNull(requester, "requester");
        this.category = Objects.requireNonNull(category, "category");
        this.hours = Objects.requireNonNull(hours, "hours");
        this.sessionDate = Objects.requireNonNull(sessionDate, "sessionDate");

        // INV-9 (R5): a member may not fulfil their own request.
        if (provider.equals(requester)) {
            throw new IllegalArgumentException("provider cannot fulfil their own request");
        }
        // INV-11 (R9): the session date must sit inside the request's fulfilment window.
        if (!window.contains(sessionDate)) {
            throw new IllegalArgumentException(
                    "session date " + sessionDate + " is outside the fulfilment window " + window);
        }

        this.state = EngagementState.PROPOSED;
    }

    /** The requester accepts the proposed engagement, moving it into ACTIVE (R14 governs this window). */
    public void accept() {
        requireState(EngagementState.PROPOSED, "accept");
        this.state = EngagementState.ACTIVE;
    }

    /**
     * One party confirms the session happened (R4).
     *
     * @return {@code true} when this is the second <em>distinct</em> confirmation and the engagement has
     *         therefore settled; {@code false} when the first confirmation has just been recorded.
     * @throws IllegalStateException if the same party confirms twice, or the engagement is not confirmable.
     */
    public boolean confirm(MemberId party) {
        if (state == EngagementState.SETTLED) {
            // INV-8: a settled engagement settles exactly once — further confirmations are no-ops.
            return false;
        }
        requireState(EngagementState.ACTIVE, "confirm");

        if (party.equals(provider) && !providerConfirmed) {
            providerConfirmed = true;
        } else if (party.equals(requester) && !requesterConfirmed) {
            requesterConfirmed = true;
        } else {
            // INV-8: one party confirming twice does not settle.
            throw new IllegalStateException("party has already confirmed this engagement: " + party);
        }

        if (providerConfirmed && requesterConfirmed) {
            this.state = EngagementState.SETTLED;
            return true;
        }
        return false;
    }

    /**
     * Cancels the engagement (R14).
     *
     * @return the resulting terminal state, so the caller knows whether a {@code LATE_CANCELLATION}
     *         record must be written against {@code cancellingParty}.
     */
    public EngagementState cancel(MemberId cancellingParty, Instant now) {
        Objects.requireNonNull(cancellingParty, "cancellingParty");
        Objects.requireNonNull(now, "now");
        // Cancellation is legal from PROPOSED or ACTIVE, but not once settled or already cancelled.
        if (state != EngagementState.PROPOSED && state != EngagementState.ACTIVE) {
            throw new IllegalStateException("cannot cancel an engagement in state " + state);
        }
        boolean late = CancellationPolicy.isLateCancellation(sessionDate, now);
        this.state = late ? EngagementState.CANCELLED_LATE : EngagementState.CANCELLED;
        return this.state;
    }

    /**
     * True when {@code party} confirming now would be the second <em>distinct</em> confirmation and hence
     * settle the engagement. Lets {@code MeithealService} run its overdraft-floor pre-check <em>before</em>
     * the state transition, so an engagement never settles into a transfer that R2 would forbid.
     */
    public boolean isSecondConfirmation(MemberId party) {
        if (state != EngagementState.ACTIVE) {
            return false;
        }
        if (party.equals(provider)) {
            return requesterConfirmed && !providerConfirmed;
        }
        if (party.equals(requester)) {
            return providerConfirmed && !requesterConfirmed;
        }
        return false;
    }

    public boolean isSettled() {
        return state == EngagementState.SETTLED;
    }

    public MemberId provider() {
        return provider;
    }

    public MemberId requester() {
        return requester;
    }

    public CategoryName category() {
        return category;
    }

    public Duration hours() {
        return hours;
    }

    public LocalDate sessionDate() {
        return sessionDate;
    }

    public EngagementState state() {
        return state;
    }

    private void requireState(EngagementState expected, String action) {
        if (state != expected) {
            throw new IllegalStateException(
                    "cannot " + action + " an engagement in state " + state + " (expected " + expected + ")");
        }
    }
}
