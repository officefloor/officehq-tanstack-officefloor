package net.officefloor.hq.app.person;

/** Whether the user now watches the person. */
public record WatchResponse(long personId, boolean watched) {
}
