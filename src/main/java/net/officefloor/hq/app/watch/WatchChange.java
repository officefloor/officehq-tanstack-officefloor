package net.officefloor.hq.app.watch;

/** What a watch did: whether the user now watches the person, and whether that changed anything. */
public record WatchChange(boolean on, boolean changed) {
}
