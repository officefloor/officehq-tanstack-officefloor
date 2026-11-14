package net.officefloor.hq.app.period;

/** A period on a sheet: its name and its from and to dates (yyyy-MM-dd), both inclusive. */
public record Period(String sheetId, String name, String from, String to) {
}
