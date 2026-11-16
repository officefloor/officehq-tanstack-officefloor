package net.officefloor.hq.app.period;

/** A period as sent to the screen: its dates as yyyy-MM-dd. */
public record PeriodResponse(long id, String sheetId, String name, String from, String to) {
    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.id(), period.sheetId(), period.name(), period.from().toString(),
                period.to().toString());
    }
}
