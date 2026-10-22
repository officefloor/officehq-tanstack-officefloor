package net.officefloor.hq.app.claim;

/** Where a claim is in its life: DRAFT, then OPEN, then SETTLED; a DRAFT or OPEN claim may be VOID.
 *  Only OPEN and SETTLED claims count toward the totals. */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID;

    /** Whether a claim with this status counts toward the totals (and is bound by a statement). */
    public boolean counts() {
        return this == OPEN || this == SETTLED;
    }

    /** Whether a claim with this status may move to the given one. */
    public boolean canBecome(ClaimStatus to) {
        return switch (to) {
            case OPEN -> this == DRAFT;
            case SETTLED -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN;
            case DRAFT -> false;
        };
    }
}
