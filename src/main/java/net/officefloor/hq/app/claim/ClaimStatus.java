package net.officefloor.hq.app.claim;

/** Where a claim is in its life: DRAFT, then OPEN, then SETTLED; a DRAFT or OPEN claim can be VOID. */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID;

    /** Whether a claim in this status may move to {@code next}. */
    public boolean allows(ClaimStatus next) {
        return switch (next) {
            case OPEN -> this == DRAFT;
            case SETTLED -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN;
            case DRAFT -> false;
        };
    }

    /** The audit event of a change to this status. */
    public String event() {
        return switch (this) {
            case OPEN -> "CLAIM_OPENED";
            case SETTLED -> "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DRAFT -> "CLAIM_DRAFTED";
        };
    }
}
