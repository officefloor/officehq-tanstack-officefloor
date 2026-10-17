package net.officefloor.hq.app.period;

/** A period as shown: its name and its from and to dates (yyyy-mm-dd). */
public record PeriodResponse(String name, String from, String to) {

    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.getName(), period.getFromDate().toString(), period.getToDate().toString());
    }
}
