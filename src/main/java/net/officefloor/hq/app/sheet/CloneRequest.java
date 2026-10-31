package net.officefloor.hq.app.sheet;

/** Body of a clone: the id of the (never used) sheet to clone to, and who clones. */
public record CloneRequest(String to, String user) {
}
