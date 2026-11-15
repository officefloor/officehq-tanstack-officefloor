package net.officefloor.hq.app.claim;

/** A claim's status. It starts DRAFT; DRAFT opens to OPEN, OPEN settles to SETTLED (or, settled by a
 *  clerk above the clerk limit, to PENDING), PENDING is approved to SETTLED or rejected back to OPEN,
 *  and DRAFT, OPEN or PENDING voids to VOID. VOID is final; SETTLED is too, but for a steward voiding it
 *  soon after its settlement (see {@link VoidClaim}). A DRAFT can be deleted to DELETED, which counts
 *  nowhere, and restored to DRAFT on the sheet's today it was deleted (see {@link RestoreClaim}). */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID, DELETED;

    /** The status after moving to {@code to}; refused when this status does not allow it. */
    public ClaimStatus moveTo(ClaimStatus to) throws ClaimStatusException {
        boolean allowed = switch (to) {
            case OPEN -> this == DRAFT || this == PENDING;
            case PENDING -> this == OPEN;
            case SETTLED -> this == OPEN || this == PENDING;
            case VOID -> this == DRAFT || this == OPEN || this == PENDING;
            case DELETED -> this == DRAFT;
            case DRAFT -> this == DELETED;
        };
        if (!allowed) {
            throw new ClaimStatusException("A " + this + " claim cannot become " + to);
        }
        return to;
    }

    /** Whether a claim with this status is open: OPEN, or PENDING (awaiting a steward's approval). */
    public static boolean isOpen(String status) {
        return OPEN.name().equals(status) || PENDING.name().equals(status);
    }

    /** The audit event recorded when a claim moves to this status. */
    public String event() {
        return switch (this) {
            case OPEN -> "CLAIM_OPENED";
            case PENDING -> "CLAIM_PENDING";
            case SETTLED -> "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DELETED -> "CLAIM_DELETED";
            case DRAFT -> "CLAIM_RESTORED";
        };
    }
}
