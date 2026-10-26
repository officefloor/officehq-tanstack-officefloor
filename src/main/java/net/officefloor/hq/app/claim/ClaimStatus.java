package net.officefloor.hq.app.claim;

/**
 * A claim's status: DRAFT → OPEN → SETTLED, and a DRAFT, OPEN or PENDING claim may become VOID. A clerk's settle above
 * the clerk limit leaves an OPEN claim PENDING; a steward's review then approves it (SETTLED) or rejects it (OPEN).
 * A steward may also void a SETTLED claim within {@link #VOID_SETTLED_DAYS} days of its settlement. A DRAFT claim may be
 * DELETED (it counts nowhere and takes no other change), and made a DRAFT again on the same sheet-today it was deleted.
 * An open of a claim whose due is above the review threshold leaves it in REVIEW (unnumbered, counted in no total) until
 * a steward's review opens it; it may be voided meanwhile.
 */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID, DELETED, REVIEW;

    /** How many days after its settlement (by the sheet's today) a steward may still void a SETTLED claim. */
    public static final int VOID_SETTLED_DAYS = 7;

    /** Whether a claim in this status may move to {@code next} by an open, settle, void, delete or restore (not a review); a
     * SETTLED claim's void is further bound to a steward and its settlement window. */
    public boolean canBecome(ClaimStatus next) {
        return switch (next) {
            case OPEN -> this == DRAFT;
            case SETTLED, PENDING -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN || this == PENDING || this == SETTLED || this == REVIEW;
            case DELETED -> this == DRAFT;
            case DRAFT -> this == DELETED;
            case REVIEW -> false; // reached only by an open above the review threshold
        };
    }

    /** Whether a claim in this status counts toward claimed, due and every total. */
    public boolean counts() {
        return this == OPEN || this == PENDING || this == SETTLED;
    }

    /** Whether a claim in this status is live: OPEN or PENDING — it goes overdue, and a hold takes it out of the totals. */
    public boolean isOpen() {
        return this == OPEN || this == PENDING;
    }
}
