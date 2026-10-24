package net.officefloor.hq.app.period;

/** A period as the screen shows it: its name, and its from and to date (YYYY-MM-DD). */
public record PeriodResponse(long id, String name, String from, String to) {

    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.getId(), period.getName(), period.getFromDate().toString(),
                period.getToDate().toString());
    }
}
