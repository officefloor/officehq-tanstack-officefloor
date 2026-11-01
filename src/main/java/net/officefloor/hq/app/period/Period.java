package net.officefloor.hq.app.period;

/**
 * A period on a sheet: its name and its from and to date (yyyy-MM-dd), inclusive, whether it is closed, and its
 * due-total fixed when closed (two decimals; null while open).
 */
public record Period(String name, String from, String to, boolean closed, String closedTotal) {
}
