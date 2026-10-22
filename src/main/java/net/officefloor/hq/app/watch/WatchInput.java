package net.officefloor.hq.app.watch;

/** A validated watch: the signed-in user and whether they are to watch the person. */
public record WatchInput(String user, boolean watched) {
}
