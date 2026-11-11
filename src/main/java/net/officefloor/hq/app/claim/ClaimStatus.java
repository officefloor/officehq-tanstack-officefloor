package net.officefloor.hq.app.claim;

/**
 * A claim's status. It starts as DRAFT, is opened (OPEN) and then settled (SETTLED). A clerk's
 * settle above the clerk limit makes it PENDING instead, until a steward approves it (SETTLED) or
 * rejects it (back to OPEN). A claim whose due is above the review threshold in force on its date goes
 * to REVIEW instead of OPEN when it is opened, until a steward reviews it (OPEN). A claim is numbered only when it is SETTLED.
 * A DRAFT, REVIEW, OPEN or PENDING claim can be voided (VOID); a SETTLED one
 * too, by a steward within days of its settlement ({@link VoidClaim}). VOID is final. A DRAFT claim can be deleted
 * (DELETED), and made a DRAFT again by a restore on the sheet's today it was deleted ({@link RestoreClaim}). Only OPEN, PENDING and SETTLED claims count toward any total.
 */
public enum ClaimStatus {
    DRAFT(null), REVIEW("CLAIM_REVIEW"), OPEN("CLAIM_OPENED"), PENDING("CLAIM_PENDING"), SETTLED("CLAIM_SETTLED"), VOID("CLAIM_VOIDED"),
    DELETED("CLAIM_DELETED");

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
            case REVIEW -> this == DRAFT;
            case OPEN -> this == DRAFT || this == REVIEW;
            case PENDING -> this == OPEN;
            case SETTLED -> this == OPEN;
            case VOID -> this == DRAFT || this == REVIEW || this == OPEN || this == PENDING || this == SETTLED;
            case DELETED -> this == DRAFT;
            case DRAFT -> this == DELETED;
        };
    }
}
