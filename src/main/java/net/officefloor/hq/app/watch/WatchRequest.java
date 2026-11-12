package net.officefloor.hq.app.watch;

/** Body of a watch toggle: the signed-in user who starts or stops watching the person. */
public record WatchRequest(String user) {
}
