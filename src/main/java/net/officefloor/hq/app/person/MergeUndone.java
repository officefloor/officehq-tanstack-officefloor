package net.officefloor.hq.app.person;

/** A merge undone: the sheet, the name of the person restored and the name of the person they had been merged into. */
public record MergeUndone(String sheetId, String name, String from) {
}
