package net.officefloor.hq.app.period;

/** A period as the screen sees it: its dates as yyyy-mm-dd, whether it is closed and, when it is,
 *  the due-total fixed when it was closed. */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal) {
    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(), period.closed(),
                period.closedTotal() == null ? null : period.closedTotal().setScale(2).toPlainString());
    }
}
