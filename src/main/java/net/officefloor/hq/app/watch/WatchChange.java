package net.officefloor.hq.app.watch;

/** A watch change once validated: whether the user starts or stops watching, and who the user is. */
public record WatchChange(boolean on, String by) {
}
