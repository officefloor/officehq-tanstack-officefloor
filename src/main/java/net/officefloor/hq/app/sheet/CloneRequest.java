package net.officefloor.hq.app.sheet;

/** Body of cloning a sheet: the id of the sheet to clone it to, and who clones it. */
public record CloneRequest(String to, String by) {
}
