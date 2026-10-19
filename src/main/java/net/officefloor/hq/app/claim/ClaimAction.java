package net.officefloor.hq.app.claim;

import java.util.Set;

/** A status change, named as in the path: the statuses it may be taken from, the status it moves
 *  the claim to and the audit event it writes. */
public enum ClaimAction {
    OPEN(Set.of(ClaimStatus.DRAFT), ClaimStatus.OPEN, "CLAIM_OPENED"),
    SETTLE(Set.of(ClaimStatus.OPEN), ClaimStatus.SETTLED, "CLAIM_SETTLED"),
    VOID(Set.of(ClaimStatus.DRAFT, ClaimStatus.OPEN), ClaimStatus.VOID, "CLAIM_VOIDED");

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
