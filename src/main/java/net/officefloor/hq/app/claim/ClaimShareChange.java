package net.officefloor.hq.app.claim;

/** A claim being shared by `user`: `share` the share given. */
public record ClaimShareChange(String user, ClaimShare share) {
}
