package net.officefloor.hq.app.sheet;

/** A sheet just cloned to another id, by whom. */
public record Clone(String from, String to, String user) {
}
