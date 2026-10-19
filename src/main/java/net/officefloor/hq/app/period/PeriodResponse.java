package net.officefloor.hq.app.period;

/** A period as the screen shows it: its name, its from and to dates as yyyy-MM-dd, whether it is
 *  closed and, once closed, the due-total it was closed at (two decimals; null while open). */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal) {
    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(), period.closed(),
                period.closedTotal() == null ? null : period.closedTotal().toPlainString());
    }
}
