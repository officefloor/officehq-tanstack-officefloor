package net.officefloor.hq.app.claim;

/** Where a claim is in its life: only OPEN and SETTLED claims count toward the totals. */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID;

    /** The status a change takes this claim to, or null when the change is not allowed from here. */
    public ClaimStatus after(ClaimStatusChange change) {
        return switch (change) {
            case OPEN -> this == DRAFT ? OPEN : null;
            case SETTLE -> this == OPEN ? SETTLED : null;
            case VOID -> this == DRAFT || this == OPEN ? VOID : null;
        };
    }
}
