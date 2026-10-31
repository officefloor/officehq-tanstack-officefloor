package net.officefloor.hq.app.claim;

/** A validated status change body. */
public record ClaimStatusChange(ClaimAction action, String user) {
}
