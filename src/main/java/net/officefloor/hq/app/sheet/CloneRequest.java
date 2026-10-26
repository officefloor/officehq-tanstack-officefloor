package net.officefloor.hq.app.sheet;

/** Clone the sheet to another sheet id (`to`), by the signed-in user (`by`). */
public record CloneRequest(String to, String by) {
}
