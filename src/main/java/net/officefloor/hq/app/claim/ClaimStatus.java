package net.officefloor.hq.app.claim;

/**
 * A claim's status: it starts DRAFT, is opened (OPEN) and then settled (SETTLED); a DRAFT or OPEN claim
 * can be voided (VOID). SETTLED and VOID are final. Only OPEN and SETTLED claims count toward totals.
 */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID;

    /** The status an action ("open", "settle", "void") moves this one to, or null when it is not allowed. */
    public ClaimStatus after(String action) {
        return switch (action) {
            case "open" -> this == DRAFT ? OPEN : null;
            case "settle" -> this == OPEN ? SETTLED : null;
            case "void" -> this == DRAFT || this == OPEN ? VOID : null;
            default -> null;
        };
    }

    /** The audit event recorded on entering this status. */
    public String event() {
        return switch (this) {
            case OPEN -> "CLAIM_OPENED";
            case SETTLED -> "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DRAFT -> "CLAIM_DRAFTED";
        };
    }
}
