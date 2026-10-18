package net.officefloor.hq.app.claim;

/** A claim's status. It starts DRAFT; DRAFT → OPEN → SETTLED; DRAFT or OPEN → VOID. A clerk settling a claim
 *  whose due is above the clerk limit moves it OPEN → PENDING instead; a steward approves it (PENDING → SETTLED)
 *  or rejects it (PENDING → OPEN). SETTLED and VOID are final. */
public enum ClaimStatus {
    DRAFT, OPEN, PENDING, SETTLED, VOID;

    /** Whether a claim in this status may move to {@code to} by opening, settling or voiding it (approving
     *  and rejecting a PENDING claim are checked by {@link #awaitsApproval()}). */
    public boolean canBecome(ClaimStatus to) {
        return switch (to) {
            case OPEN -> this == DRAFT;
            case SETTLED, PENDING -> this == OPEN;
            case VOID -> this == DRAFT || this == OPEN;
            case DRAFT -> false;
        };
    }

    /** Whether a claim in this status waits for a steward to approve or reject it. */
    public boolean awaitsApproval() {
        return this == PENDING;
    }

    /** Whether a claim in this status is live: OPEN, or PENDING (which counts as open everywhere). */
    public boolean live() {
        return this == OPEN || this == PENDING;
    }

    /** Whether a claim in this status counts toward claimed, due-total and every total. */
    public boolean counts() {
        return live() || this == SETTLED;
    }
}
