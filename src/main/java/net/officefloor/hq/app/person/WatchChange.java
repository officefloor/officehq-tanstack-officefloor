package net.officefloor.hq.app.person;

/** A watch toggle by this user; `watching` is whether they watch the person once it is applied. */
public record WatchChange(String user, boolean watching) {
}
