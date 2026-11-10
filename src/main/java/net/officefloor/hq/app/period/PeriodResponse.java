package net.officefloor.hq.app.period;

/**
 * A period, as the screen reads it: its name, its from and to dates (yyyy-MM-dd), whether it is
 * closed, and the due-total fixed when it was closed ("" while open).
 */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal) {
    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(),
                period.closed(), period.closedTotal() == null ? "" : period.closedTotal().setScale(2).toPlainString());
    }
}
