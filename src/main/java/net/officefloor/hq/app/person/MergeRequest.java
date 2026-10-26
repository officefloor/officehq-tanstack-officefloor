package net.officefloor.hq.app.person;

/** The id of the person to merge into, and the signed-in user (`by`) doing it. */
public record MergeRequest(Long into, String by) {
}
