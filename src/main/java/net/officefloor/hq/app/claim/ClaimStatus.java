package net.officefloor.hq.app.claim;

/**
 * Where a claim is in its life: DRAFT, then OPEN, then SETTLED; a DRAFT, OPEN or PENDING claim can be VOID, and a
 * SETTLED one too, by a steward within {@link #VOID_SETTLED_DAYS} days of its settlement. A clerk's
 * settle of a claim whose due is above the clerk limit makes it PENDING instead, until a steward approves it (SETTLED)
 * or rejects it (back to OPEN).
 */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID;

    /** How many days after its settlement (by the sheet's today) a steward may still void a SETTLED claim. */
    public static final int VOID_SETTLED_DAYS = 7;

    /** Whether a claim in this status may move to {@code next}. */
    public boolean allows(ClaimStatus next) {
        return switch (next) {
            case OPEN -> this == DRAFT || this == PENDING;
            case PENDING -> this == OPEN;
            case SETTLED -> this == OPEN || this == PENDING;
            case VOID -> this == DRAFT || this == OPEN || this == PENDING || this == SETTLED;
            case DRAFT -> false;
        };
    }

    /** Whether a claim in this status is open: OPEN, or PENDING, which is handled like OPEN in every total. */
    public boolean isOpen() {
        return this == OPEN || this == PENDING;
    }

    /** Whether a claim in this status counts toward the totals: an open or a SETTLED one. */
    public boolean counts() {
        return isOpen() || this == SETTLED;
    }

    /** The audit event of a change from {@code from} to this status. */
    public String event(ClaimStatus from) {
        if (from == PENDING) {
            return this == SETTLED ? "CLAIM_APPROVED" : this == OPEN ? "CLAIM_REJECTED" : event();
        }
        return event();
    }

    /** The audit event of a change to this status. */
    public String event() {
        return switch (this) {
            case OPEN -> "CLAIM_OPENED";
            case PENDING -> "CLAIM_PENDING";
            case SETTLED -> "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DRAFT -> "CLAIM_DRAFTED";
        };
    }
}
