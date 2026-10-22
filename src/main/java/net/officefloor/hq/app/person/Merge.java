package net.officefloor.hq.app.person;

/** A merge that went through: the sheet, the merged person's name and the name of the person merged into. */
public record Merge(String sheetId, String from, String into) {
}
