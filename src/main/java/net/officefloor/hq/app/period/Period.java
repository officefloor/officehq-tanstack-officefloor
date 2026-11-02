package net.officefloor.hq.app.period;

/** A sheet's period: its name and its from- and to-date (yyyy-mm-dd), both inclusive. */
public record Period(long id, String name, String from, String to) {
}
