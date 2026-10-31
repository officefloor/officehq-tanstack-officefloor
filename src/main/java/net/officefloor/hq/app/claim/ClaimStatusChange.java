package net.officefloor.hq.app.claim;

/** A validated status change body; `reason` is a rejection's reason (null for any other action). */
public record ClaimStatusChange(ClaimAction action, String user, String reason) {
    public ClaimStatusChange(ClaimAction action, String user) {
        this(action, user, null);
    }
}
