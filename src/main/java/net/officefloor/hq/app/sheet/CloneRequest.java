package net.officefloor.hq.app.sheet;

/** The body of a clone of a sheet: the id of the (never used) sheet to clone it to. */
public record CloneRequest(String toSheetId) {
}
