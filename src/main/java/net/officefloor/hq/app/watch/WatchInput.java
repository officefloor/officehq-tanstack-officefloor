package net.officefloor.hq.app.watch;

/** A validated watch request: whether the signed-in user starts (true) or stops (false) watching. */
public record WatchInput(boolean on, String user) {
}
