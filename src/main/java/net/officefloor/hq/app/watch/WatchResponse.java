package net.officefloor.hq.app.watch;

/** Whether the user watches the person; changed says the request moved it (false: it already held). */
public record WatchResponse(long personId, boolean watched, boolean changed) {
}
