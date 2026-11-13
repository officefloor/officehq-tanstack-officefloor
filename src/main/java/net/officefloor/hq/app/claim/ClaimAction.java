package net.officefloor.hq.app.claim;

/**
 * A change of a claim's status: the status it may start from, the status it ends in, and its audit event. PEND is
 * never asked for by name: it is what a clerk's settle becomes when the due is above the clerk limit.
 */
public enum ClaimAction {
    OPEN("open", ClaimStatus.OPEN, "CLAIM_OPENED", ClaimStatus.DRAFT),
    SETTLE("settle", ClaimStatus.SETTLED, "CLAIM_SETTLED", ClaimStatus.OPEN),
    PEND(null, ClaimStatus.PENDING, "CLAIM_PENDING", ClaimStatus.OPEN),
    APPROVE("approve", ClaimStatus.SETTLED, "CLAIM_APPROVED", ClaimStatus.PENDING),
    REJECT("reject", ClaimStatus.OPEN, "CLAIM_REJECTED", ClaimStatus.PENDING),
    VOID("void", ClaimStatus.VOID, "CLAIM_VOIDED", ClaimStatus.DRAFT, ClaimStatus.OPEN, ClaimStatus.PENDING);

    private final String path;
    private final ClaimStatus to;
    private final String event;
    private final ClaimStatus[] from;

    ClaimAction(String path, ClaimStatus to, String event, ClaimStatus... from) {
        this.path = path;
        this.to = to;
        this.event = event;
        this.from = from;
    }

    /** The action named in the path, or null. */
    public static ClaimAction of(String path) {
        for (ClaimAction action : values()) {
            if (action.path != null && action.path.equals(path)) {
                return action;
            }
        }
        return null;
    }

    public boolean allowedFrom(ClaimStatus status) {
        for (ClaimStatus s : from) {
            if (s == status) {
                return true;
            }
        }
        return false;
    }

    public ClaimStatus to() {
        return to;
    }

    public String event() {
        return event;
    }

    /** Whether the action fixes the claim's due: settling, or approving a pending settle. */
    public boolean settles() {
        return to == ClaimStatus.SETTLED;
    }
}
