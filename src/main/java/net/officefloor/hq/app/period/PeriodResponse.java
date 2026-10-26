package net.officefloor.hq.app.period;

/** A period as the screen sees it: the dates as yyyy-mm-dd. */
public record PeriodResponse(String name, String from, String to) {

    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString());
    }
}
