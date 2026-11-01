package net.officefloor.hq.app.claim;

/**
 * Where a claim is in its life: only OPEN, PENDING and SETTLED claims count toward the totals. A clerk's settle of a
 * due above the clerk limit makes it PENDING, which a steward approves (SETTLED) or rejects (back to OPEN).
 */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID;

    /** The status a change takes this claim to, or null when the change is not allowed from here. */
    public ClaimStatus after(ClaimStatusChange change) {
        return switch (change) {
            case OPEN -> this == DRAFT ? OPEN : null;
            case SETTLE -> this == OPEN ? SETTLED : null;
            case PEND -> this == OPEN ? PENDING : null;
            case APPROVE -> this == PENDING ? SETTLED : null;
            case REJECT -> this == PENDING ? OPEN : null;
            case VOID -> this == DRAFT || this == OPEN || this == PENDING ? VOID : null;
        };
    }
}
