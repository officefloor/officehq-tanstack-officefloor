package net.officefloor.hq.app.claim;

/**
 * A claim's status: it starts DRAFT, is opened (OPEN) and then settled (SETTLED); a clerk settling one above the
 * clerk limit leaves it PENDING, which a steward approves (SETTLED) or rejects (back to OPEN). A DRAFT, OPEN or
 * PENDING claim can be voided (VOID), and a steward may void a SETTLED one within 7 days of its settlement. VOID is final. A DRAFT
 * can be deleted (DELETED) and, on the sheet's today it was deleted, restored (DRAFT again). Only OPEN, PENDING and SETTLED claims count toward
 * totals. A claim whose due is above the sheet's review threshold in force on its date goes to REVIEW instead of OPEN
 * when opened, with no number; a steward reviews it (OPEN). A claim takes its number when it is settled (or approved). A REVIEW claim counts in no total and can be
 * voided.
 */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID, DELETED, REVIEW;

    /** The status an action ("open", "review", "settle", "approve", "reject", "void", "delete",
     *  "restore") moves this one to, or null when it is not
     *  allowed. A settle above the clerk limit by a clerk ends in PENDING instead. */
    public ClaimStatus after(String action) {
        return switch (action) {
            case "open" -> this == DRAFT ? OPEN : null;
            case "review" -> this == REVIEW ? OPEN : null;
            case "settle" -> this == OPEN ? SETTLED : null;
            case "approve" -> this == PENDING ? SETTLED : null;
            case "reject" -> this == PENDING ? OPEN : null;
            case "void" -> this == DRAFT || this == REVIEW || this == OPEN || this == PENDING || this == SETTLED ? VOID : null;
            case "delete" -> this == DRAFT ? DELETED : null;
            case "restore" -> this == DELETED ? DRAFT : null;
            default -> null;
        };
    }

    /** Whether a claim in this status is open: OPEN or PENDING (it goes overdue, and a hold takes it out of the
     *  totals). */
    public boolean isOpen() {
        return this == OPEN || this == PENDING;
    }

    /** The audit event recorded on entering this status. */
    public String event() {
        return switch (this) {
            case OPEN -> "CLAIM_OPENED";
            case PENDING -> "CLAIM_PENDING";
            case SETTLED -> "CLAIM_SETTLED";
            case VOID -> "CLAIM_VOIDED";
            case DRAFT -> "CLAIM_DRAFTED";
            case DELETED -> "CLAIM_DELETED";
            case REVIEW -> "CLAIM_REVIEW";
        };
    }
}
