package net.officefloor.hq.app.person;

/** Start (`on` true) or stop watching a person, as the signed-in user (`by`). */
public record WatchRequest(Boolean on, String by) {
}
