package net.officefloor.hq.app.claim;

/**
 * A claim's status. It starts as DRAFT, is opened (OPEN) and then settled (SETTLED); a DRAFT or
 * OPEN claim can be voided (VOID). SETTLED and VOID are final. Only OPEN and SETTLED claims count
 * toward any total.
 */
public enum ClaimStatus {
    DRAFT(null), OPEN("CLAIM_OPENED"), SETTLED("CLAIM_SETTLED"), VOID("CLAIM_VOIDED");

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
            case SETTLED -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN;
            case DRAFT -> false;
        };
    }
}
