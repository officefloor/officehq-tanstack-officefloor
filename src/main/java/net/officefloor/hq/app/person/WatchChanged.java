package net.officefloor.hq.app.person;

/** Whether a watch or unwatch changed anything (false: the user already was, or was not, watching). */
public record WatchChanged(boolean changed) {
}
