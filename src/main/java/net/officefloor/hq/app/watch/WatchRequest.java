package net.officefloor.hq.app.watch;

/** A watch as the screen sends it: whether the signed-in user is to watch the person (true) or stop (false). */
public record WatchRequest(Boolean on) {
}
