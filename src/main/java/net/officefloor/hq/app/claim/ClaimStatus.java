package net.officefloor.hq.app.claim;

/** Where a claim stands. Only OPEN and SETTLED claims count toward the totals. */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID;

    /** Whether a claim may move from this status to {@code next}: DRAFT to OPEN, OPEN to SETTLED, and
     *  DRAFT or OPEN to VOID. SETTLED and VOID are final. */
    public boolean canBecome(ClaimStatus next) {
        return switch (next) {
            case OPEN -> this == DRAFT;
            case SETTLED -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN;
            case DRAFT -> false;
        };
    }

    /** The audit event recorded when a claim moves to this status. */
    public String event() {
        return switch (this) {
            case OPEN -> "CLAIM_OPENED";
            case SETTLED -> "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DRAFT -> "CLAIM_DRAFTED";
        };
    }
}
