package net.officefloor.hq.app.claim;

/** A change of a claim's status, named by the last path segment, with the audit event it writes. */
public enum ClaimStatusChange {
    OPEN("CLAIM_OPENED"), SETTLE("CLAIM_SETTLED"), VOID("CLAIM_VOIDED");

    private final String event;

    ClaimStatusChange(String event) {
        this.event = event;
    }

    public String event() {
        return event;
    }

    /** The change named in the path (open, settle or void), or null when there is no such change. */
    public static ClaimStatusChange of(String name) {
        for (ClaimStatusChange change : values()) {
            if (change.name().equalsIgnoreCase(name)) {
                return change;
            }
        }
        return null;
    }
}
