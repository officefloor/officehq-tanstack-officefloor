package net.officefloor.hq.app.claim;

/** Where a claim is in its life: DRAFT → OPEN → SETTLED (or, settled by a clerk over the clerk
 *  limit, OPEN → PENDING, which a steward approves to SETTLED or rejects back to OPEN), and DRAFT,
 *  OPEN or PENDING → VOID; a steward may also void a SETTLED claim within 7 days of its settlement.
 *  A DRAFT whose due is above the sheet's review threshold goes REVIEW when opened, until a steward
 *  reviews it to OPEN (or it is voided); a REVIEW claim counts nowhere but in the status counts.
 *  A DRAFT can be DELETED, and restored to DRAFT on the same sheet's today; a DELETED claim counts nowhere. */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID, DELETED, REVIEW;

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
