package net.officefloor.hq.app.claim;

/**
 * Where a claim is in its life: only OPEN, PENDING and SETTLED claims count toward the totals. A clerk's settle of a
 * due above the clerk limit makes it PENDING, which a steward approves (SETTLED) or rejects (back to OPEN). A steward
 * may void a SETTLED claim within 7 days of its settlement. A DRAFT may be DELETED (counting nowhere, not editable,
 * openable nor voidable), and restored to a DRAFT on the sheet-today it was deleted. A DRAFT whose due is above the
 * review threshold in force on its date goes to REVIEW when opened (counting nowhere, not editable, settleable nor
 * splittable, but voidable), which a steward's review makes OPEN.
 */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID, DELETED, REVIEW;

    /** The status a change takes this claim to, or null when the change is not allowed from here. */
    public ClaimStatus after(ClaimStatusChange change) {
        return switch (change) {
            case OPEN -> this == DRAFT ? OPEN : null;
            case HOLD_FOR_REVIEW -> this == DRAFT ? REVIEW : null;
            case REVIEW -> this == REVIEW ? OPEN : null;
            case SETTLE -> this == OPEN ? SETTLED : null;
            case PEND -> this == OPEN ? PENDING : null;
            case APPROVE -> this == PENDING ? SETTLED : null;
            case REJECT -> this == PENDING ? OPEN : null;
            case VOID -> this == DRAFT || this == OPEN || this == PENDING || this == SETTLED || this == REVIEW ? VOID : null;
            case DELETE -> this == DRAFT ? DELETED : null;
            case RESTORE -> this == DELETED ? DRAFT : null;
        };
    }
}
