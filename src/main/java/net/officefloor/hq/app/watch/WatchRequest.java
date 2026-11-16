package net.officefloor.hq.app.watch;

/** Body of watching a person: whether to start (true) or stop (false) watching them, and who does it. */
public record WatchRequest(Boolean on, String by) {
}
