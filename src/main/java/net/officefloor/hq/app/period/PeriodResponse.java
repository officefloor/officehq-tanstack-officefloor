package net.officefloor.hq.app.period;

/** A period as the screen sees it: the dates as yyyy-mm-dd, and the due-total fixed when closed (null while open). */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal) {

    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(), period.closed(),
                period.closedTotal() == null ? null : period.closedTotal().toPlainString());
    }
}
