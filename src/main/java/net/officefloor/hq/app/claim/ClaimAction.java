package net.officefloor.hq.app.claim;

/** A status change asked of a claim: the status it moves to, the statuses it may move from, and
 *  the audit event it writes. */
public enum ClaimAction {
    OPEN(ClaimStatus.OPEN, "CLAIM_OPENED", ClaimStatus.DRAFT),
    SETTLE(ClaimStatus.SETTLED, "CLAIM_SETTLED", ClaimStatus.OPEN),
    APPROVE(ClaimStatus.SETTLED, "CLAIM_APPROVED", ClaimStatus.PENDING),
    REJECT(ClaimStatus.OPEN, "CLAIM_REJECTED", ClaimStatus.PENDING),
    VOID(ClaimStatus.VOID, "CLAIM_VOIDED", ClaimStatus.DRAFT, ClaimStatus.OPEN, ClaimStatus.PENDING);

    private final ClaimStatus to;
    private final String event;
    private final ClaimStatus[] from;

    ClaimAction(ClaimStatus to, String event, ClaimStatus... from) {
        this.to = to;
        this.event = event;
        this.from = from;
    }

    public ClaimStatus to() { return to; }
    public String event() { return event; }

    public boolean allowedFrom(ClaimStatus status) {
        for (ClaimStatus s : from) {
            if (s == status) return true;
        }
        return false;
    }
}
