package net.officefloor.hq.app.watch;

/** A stored toggle: who toggled, and whether they now watch the person. */
public record Watching(String user, boolean watched) {
}
