package net.officefloor.hq.app.claim;

/** Where a claim is in its life: DRAFT, then OPEN, then SETTLED; a clerk's settle of a claim due above the clerk
 *  limit makes it PENDING instead, until a steward approves it (SETTLED) or rejects it (OPEN again); a DRAFT, OPEN
 *  or PENDING claim may be VOID, and a steward may void a SETTLED one within 7 days of its settlement (checked in
 *  ClaimStatusChange). A DRAFT claim may be DELETED, and restored to a DRAFT on the same sheet-today (checked in
 *  ClaimStatusChange). Only OPEN, PENDING and SETTLED claims count toward the totals. */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID, DELETED;

    /** Whether a claim with this status counts toward the totals (and is bound by a statement). */
    public boolean counts() {
        return this == OPEN || this == PENDING || this == SETTLED;
    }

    /** Whether a claim with this status is still to be settled: OPEN or PENDING (it goes overdue, bears the late
     *  charge). */
    public boolean unsettled() {
        return this == OPEN || this == PENDING;
    }

    /** Whether a claim with this status may move to the given one by opening, settling, voiding, deleting or
     *  restoring it (approving
     *  and rejecting a PENDING claim are checked on their own). */
    public boolean canBecome(ClaimStatus to) {
        return switch (to) {
            case OPEN -> this == DRAFT;
            case SETTLED, PENDING -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN || this == PENDING;
            case DELETED -> this == DRAFT;
            case DRAFT -> this == DELETED;
        };
    }
}
