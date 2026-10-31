package net.officefloor.hq.app.claim;

/** Where a claim is in its life: DRAFT → OPEN → SETTLED (or, settled by a clerk over the clerk
 *  limit, OPEN → PENDING, which a steward approves to SETTLED or rejects back to OPEN), and DRAFT,
 *  OPEN or PENDING → VOID. */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID;

    /** Only OPEN, PENDING and SETTLED claims count toward claimed, due-total and every total. */
    public boolean counts() {
        return this == OPEN || this == PENDING || this == SETTLED;
    }

    /** Whether the claim is still open: OPEN, or PENDING (handled like an OPEN one: it goes overdue,
     *  bears the late charge and a hold takes it out of the totals). */
    public boolean isOpen() {
        return this == OPEN || this == PENDING;
    }
}
