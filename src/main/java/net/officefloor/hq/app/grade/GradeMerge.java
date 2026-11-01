package net.officefloor.hq.app.grade;

/** A validated grade merge on a sheet. */
public record GradeMerge(String sheetId, String from, String into, String by) {
}
