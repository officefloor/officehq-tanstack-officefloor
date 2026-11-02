package net.officefloor.hq.app.period;

/** A sheet's period: its name and its from- and to-date (yyyy-mm-dd), both inclusive, whether it is
 *  closed, and the due-total fixed when it was closed (two decimals; null while open). */
public record Period(long id, String name, String from, String to, boolean closed, String closedTotal) {
}
