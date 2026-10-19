package net.officefloor.hq.app.claim;

import java.util.Set;

/** A status change, named as in the path: the statuses it may be taken from, the status it moves
 *  the claim to and the audit event it writes. A settle the clerk limit sends to PENDING writes CLAIM_PENDING. */
public enum ClaimAction {
    OPEN(Set.of(ClaimStatus.DRAFT), ClaimStatus.OPEN, "CLAIM_OPENED"),
    SETTLE(Set.of(ClaimStatus.OPEN), ClaimStatus.SETTLED, "CLAIM_SETTLED"),
    APPROVE(Set.of(ClaimStatus.PENDING), ClaimStatus.SETTLED, "CLAIM_APPROVED"),
    REJECT(Set.of(ClaimStatus.PENDING), ClaimStatus.OPEN, "CLAIM_REJECTED"),
    VOID(Set.of(ClaimStatus.DRAFT, ClaimStatus.OPEN, ClaimStatus.PENDING), ClaimStatus.VOID, "CLAIM_VOIDED");

    /** The event of a settle the clerk limit sent to PENDING. */
    public static final String PENDING_EVENT = "CLAIM_PENDING";

    private final Set<ClaimStatus> from;
    private final ClaimStatus to;
    private final String event;

    ClaimAction(Set<ClaimStatus> from, ClaimStatus to, String event) {
        this.from = from;
        this.to = to;
        this.event = event;
    }

    public boolean allowedFrom(ClaimStatus status) { return from.contains(status); }
    public ClaimStatus to() { return to; }
    public String event() { return event; }
}
