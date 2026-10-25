package net.officefloor.hq.app.claim;

/**
 * A claim's status: DRAFT → OPEN → SETTLED, and a DRAFT, OPEN or PENDING claim may become VOID. A clerk's settle above
 * the clerk limit leaves an OPEN claim PENDING; a steward's review then approves it (SETTLED) or rejects it (OPEN).
 */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID;

    /** Whether a claim in this status may move to {@code next} by an open, settle or void (not a review). */
    public boolean canBecome(ClaimStatus next) {
        return switch (next) {
            case OPEN -> this == DRAFT;
            case SETTLED, PENDING -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN || this == PENDING;
            case DRAFT -> false;
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
