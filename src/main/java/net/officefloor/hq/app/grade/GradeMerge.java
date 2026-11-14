package net.officefloor.hq.app.grade;

/** A grade merged into another on the sheet. */
public record GradeMerge(String sheetId, String from, String into) {
}
