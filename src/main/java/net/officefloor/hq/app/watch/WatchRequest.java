package net.officefloor.hq.app.watch;

/** Body of POST /api/sheets/{sheet}/people/{person}/watch: whether the signed-in user is to watch the person. */
public record WatchRequest(Boolean on) {
}
