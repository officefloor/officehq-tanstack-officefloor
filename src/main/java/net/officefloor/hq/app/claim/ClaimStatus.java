package net.officefloor.hq.app.claim;

/** A claim's status. It starts DRAFT; DRAFT → OPEN → SETTLED; DRAFT or OPEN → VOID. SETTLED and VOID are final. */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID;

    /** Whether a claim in this status may move to {@code to}. */
    public boolean canBecome(ClaimStatus to) {
        return switch (to) {
            case OPEN -> this == DRAFT;
            case SETTLED -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN;
            case DRAFT -> false;
        };
    }

    /** Whether a claim in this status counts toward claimed, due-total and every total. */
    public boolean counts() {
        return this == OPEN || this == SETTLED;
    }
}
