package net.officefloor.hq.app.holiday;

/** A holiday on a sheet (yyyy-MM-dd), and who added it. */
public record Holiday(String sheetId, String date, String by) {
}
