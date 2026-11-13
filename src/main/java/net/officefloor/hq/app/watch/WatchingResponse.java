package net.officefloor.hq.app.watch;

/** Whether the signed-in user now watches the person. */
public record WatchingResponse(long personId, boolean watched) {
}
