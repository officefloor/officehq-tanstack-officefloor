package net.officefloor.hq.app.person;

/** Whether to put the person on hold (true) or take them off it (false), and the signed-in user (`by`) doing it. */
public record HoldRequest(Boolean held, String by) {
}
