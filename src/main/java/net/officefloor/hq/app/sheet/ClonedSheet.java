package net.officefloor.hq.app.sheet;

/** A clone made: the sheet cloned ({@code fromSheetId}) and the sheet it was cloned to ({@code toSheetId}). */
public record ClonedSheet(String fromSheetId, String toSheetId) {
}
