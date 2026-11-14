package net.officefloor.hq.app.claim;

/** A claim's status. It starts DRAFT; DRAFT opens to OPEN, OPEN settles to SETTLED, and DRAFT or
 *  OPEN voids to VOID. SETTLED and VOID are final. */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID;

    /** The status after moving to {@code to}; refused when this status does not allow it. */
    public ClaimStatus moveTo(ClaimStatus to) throws ClaimStatusException {
        boolean allowed = switch (to) {
            case OPEN -> this == DRAFT;
            case SETTLED -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN;
            case DRAFT -> false;
        };
        if (!allowed) {
            throw new ClaimStatusException("A " + this + " claim cannot become " + to);
        }
        return to;
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
