package net.officefloor.hq.app.period;

/** A period: its name and its from and to dates (YYYY-MM-DD, both inclusive); whether it is closed,
 *  and the due-total fixed when it was closed ("" while open). */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal) {
}
