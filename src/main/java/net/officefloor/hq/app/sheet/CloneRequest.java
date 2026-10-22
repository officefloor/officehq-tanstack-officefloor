package net.officefloor.hq.app.sheet;

/** Body of a sheet clone: who is cloning it, and the id of the sheet to clone it to. */
public record CloneRequest(String user, String toSheet) {
}
