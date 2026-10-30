package net.officefloor.hq.app.claim;

/** A claim's status. It starts DRAFT; DRAFT -> OPEN -> SETTLED, and DRAFT or OPEN -> VOID. */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID;

    /** Whether a claim in this status may move to the target status. */
    public boolean canBecome(ClaimStatus target) {
        return switch (target) {
            case OPEN -> this == DRAFT;
            case SETTLED -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN;
            case DRAFT -> false;
        };
    }

    /** Only OPEN and SETTLED claims count toward claimed and every total. */
    public boolean counts() {
        return this == OPEN || this == SETTLED;
    }
}
