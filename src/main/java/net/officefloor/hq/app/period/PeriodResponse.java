package net.officefloor.hq.app.period;

/** A period as the screen shows it: its name, its from and to date (YYYY-MM-DD), whether it is closed and the
 *  due-total fixed when it was closed (null while open). */
public record PeriodResponse(long id, String name, String from, String to, boolean closed, String closedTotal) {

    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.getId(), period.getName(), period.getFromDate().toString(),
                period.getToDate().toString(), period.isClosed(),
                period.getClosedTotal() == null ? null : period.getClosedTotal().toPlainString());
    }
}
