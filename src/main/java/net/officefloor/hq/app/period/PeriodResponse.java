package net.officefloor.hq.app.period;

/** A period as the screen shows it: the name, the from and to dates (yyyy-mm-dd), whether it is closed and the
 *  due-total fixed when it was closed (null while open). */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal) {
    public static PeriodResponse from(Period p) {
        return new PeriodResponse(p.name(), p.from().toString(), p.to().toString(), p.closed(),
                p.closedTotal() != null ? p.closedTotal().toPlainString() : null);
    }
}
