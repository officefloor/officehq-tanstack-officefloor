package net.officefloor.hq.app.claim;

/**
 * A claim's status. It starts DRAFT; DRAFT -> OPEN -> SETTLED, and DRAFT or OPEN -> VOID. A clerk
 * settling above the clerk limit sends an OPEN claim to PENDING instead: a steward approves it
 * (-> SETTLED) or rejects it (-> OPEN); a PENDING claim may also be voided, and a steward may void a SETTLED claim
 * within 7 days of its settlement.
 */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID;

    /** Whether a claim in this status may move to the target status. */
    public boolean canBecome(ClaimStatus target) {
        return switch (target) {
            case OPEN -> this == DRAFT;
            case SETTLED, PENDING -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN || this == PENDING || this == SETTLED;
            case DRAFT -> false;
        };
    }

    /** Only OPEN, PENDING and SETTLED claims count toward claimed and every total. */
    public boolean counts() {
        return this == OPEN || this == PENDING || this == SETTLED;
    }

    /** OPEN and PENDING claims are open: their due is live, they go overdue and a hold covering their date takes them out of the totals (unless bound). */
    public boolean isOpen() {
        return this == OPEN || this == PENDING;
    }
}
