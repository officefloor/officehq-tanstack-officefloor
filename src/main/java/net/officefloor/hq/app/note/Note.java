package net.officefloor.hq.app.note;

/** A note of the sheet: its text, who added it, and the sheet's today (yyyy-MM-dd) when they did. */
public record Note(String text, String by, String on) {
}
