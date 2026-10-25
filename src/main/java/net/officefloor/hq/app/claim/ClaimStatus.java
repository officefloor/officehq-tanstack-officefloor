package net.officefloor.hq.app.claim;

/** A claim's status: DRAFT → OPEN → SETTLED, and a DRAFT or OPEN claim may become VOID. */
public enum ClaimStatus {
    DRAFT, OPEN, SETTLED, VOID;

    /** Whether a claim in this status may move to {@code next}. */
    public boolean canBecome(ClaimStatus next) {
        return switch (next) {
            case OPEN -> this == DRAFT;
            case SETTLED -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN;
            case DRAFT -> false;
        };
    }

    /** Whether a claim in this status counts toward claimed, due and every total. */
    public boolean counts() {
        return this == OPEN || this == SETTLED;
    }
}
