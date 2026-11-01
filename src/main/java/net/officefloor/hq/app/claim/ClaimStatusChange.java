package net.officefloor.hq.app.claim;

/** A change of a claim's status, named by the last path segment, with the audit event it writes. */
public enum ClaimStatusChange {
    OPEN("CLAIM_OPENED"), SETTLE("CLAIM_SETTLED"), VOID("CLAIM_VOIDED"), APPROVE("CLAIM_APPROVED"),
    REJECT("CLAIM_REJECTED"), DELETE("CLAIM_DELETED"), RESTORE("CLAIM_RESTORED"),
    /** Not named in a path: a clerk's settle of a due above the clerk limit becomes this. */
    PEND("CLAIM_PENDING");

    private final String event;

    ClaimStatusChange(String event) {
        this.event = event;
    }

    public String event() {
        return event;
    }

    /** The change named in the path (open, settle, void, approve, reject, delete or restore), or null when there is no such change. */
    public static ClaimStatusChange of(String name) {
        for (ClaimStatusChange change : values()) {
            if (change != PEND && change.name().equalsIgnoreCase(name)) {
                return change;
            }
        }
        return null;
    }
}
