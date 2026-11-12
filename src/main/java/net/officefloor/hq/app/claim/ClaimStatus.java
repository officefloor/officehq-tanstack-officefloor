package net.officefloor.hq.app.claim;

/** Where a claim stands. Only OPEN, PENDING and SETTLED claims count toward the totals; a PENDING claim
 *  (settled by a clerk above the clerk limit, awaiting a steward) is handled as an OPEN one, except
 *  that a statement does not bind it while it is PENDING. */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID;

    /** Whether a claim may move from this status to {@code next}: DRAFT to OPEN, OPEN to PENDING or
     *  SETTLED, PENDING to SETTLED (approved) or OPEN (rejected), and DRAFT, OPEN or PENDING to VOID;
     *  SETTLED to VOID too, though only a steward within days of settling it (see {@link CheckClerkLimit}).
     *  VOID is final. */
    public boolean canBecome(ClaimStatus next) {
        return switch (next) {
            case OPEN -> this == DRAFT || this == PENDING;
            case PENDING -> this == OPEN;
            case SETTLED -> this == OPEN || this == PENDING;
            case VOID -> this == DRAFT || this == OPEN || this == PENDING || this == SETTLED;
            case DRAFT -> false;
        };
    }

    /** Whether a claim of this status may move to another person: DRAFT, OPEN or PENDING. */
    public boolean canMove() {
        return this == DRAFT || this == OPEN || this == PENDING;
    }

    /** Whether the claim is open: OPEN, or PENDING, which is handled as open. */
    public boolean isOpen() {
        return this == OPEN || this == PENDING;
    }

    /** The audit event recorded when a claim moves to this status. */
    public String event() {
        return switch (this) {
            case OPEN -> "CLAIM_OPENED";
            case PENDING -> "CLAIM_PENDING";
            case SETTLED -> "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DRAFT -> "CLAIM_DRAFTED";
        };
    }

    /** The audit event recorded when a claim moves from {@code from} to this status: a PENDING claim
     *  settled is CLAIM_APPROVED, and one moved back to OPEN is CLAIM_REJECTED. */
    public String event(ClaimStatus from) {
        if (from == PENDING && this == SETTLED) {
            return "CLAIM_APPROVED";
        }
        if (from == PENDING && this == OPEN) {
            return "CLAIM_REJECTED";
        }
        return event();
    }
}
