package net.officefloor.hq.app.period;

/** A period as the screen shows it: the name and the from and to dates (yyyy-mm-dd). */
public record PeriodResponse(String name, String from, String to) {
    public static PeriodResponse from(Period p) {
        return new PeriodResponse(p.name(), p.from().toString(), p.to().toString());
    }
}
