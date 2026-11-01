package net.officefloor.hq.app.period;

/** A period on a sheet: its name and its from and to date (yyyy-MM-dd), inclusive. */
public record Period(String name, String from, String to) {
}
