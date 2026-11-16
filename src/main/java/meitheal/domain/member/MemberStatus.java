package meitheal.domain.member;

/**
 * The lifecycle status of a member.
 *
 * <p>R6: a suspended member may not create requests, offer skills, or enter new engagements, but must still
 * be able to receive credits for work completed before suspension. Our team's decision (Design Report §1.2,
 * decision D-a) reads the prohibition as attaching to the <em>creation</em> of new commitments, not to the
 * <em>completion</em> of an engagement that already existed when suspension happened.
 */
public enum MemberStatus {
    ACTIVE,
    SUSPENDED;

    public boolean canCreateNewCommitments() {
        return this == ACTIVE;
    }
}
