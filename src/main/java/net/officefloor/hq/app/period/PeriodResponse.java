package net.officefloor.hq.app.period;

/** A period as shown: its name, its from and to dates (yyyy-mm-dd), whether it is closed and, once
 *  closed, the due-total fixed then (two decimals). */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal) {

    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.getName(), period.getFromDate().toString(), period.getToDate().toString(),
                period.isClosed(), period.getClosedTotal() == null ? null : period.getClosedTotal().toPlainString());
    }
}
