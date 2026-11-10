package net.officefloor.hq.app.claim;

/**
 * A claim's status. It starts as DRAFT, is opened (OPEN) and then settled (SETTLED). A clerk's
 * settle above the clerk limit makes it PENDING instead, until a steward approves it (SETTLED) or
 * rejects it (back to OPEN). A DRAFT, OPEN or PENDING claim can be voided (VOID); a SETTLED one
 * too, by a steward within days of its settlement ({@link VoidClaim}). VOID is final. Only OPEN, PENDING and SETTLED claims count toward any total.
 */
public enum ClaimStatus {
    DRAFT(null), OPEN("CLAIM_OPENED"), PENDING("CLAIM_PENDING"), SETTLED("CLAIM_SETTLED"), VOID("CLAIM_VOIDED");

    private final String auditEvent;

    ClaimStatus(String auditEvent) {
        this.auditEvent = auditEvent;
    }

    /** The audit event written when a claim moves into this status. */
    public String auditEvent() {
        return auditEvent;
    }

    /** Whether a claim in this status may move to {@code next}. */
    public boolean canMoveTo(ClaimStatus next) {
        return switch (next) {
            case OPEN -> this == DRAFT;
            case PENDING -> this == OPEN;
            case SETTLED -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN || this == PENDING || this == SETTLED;
            case DRAFT -> false;
        };
    }
}
