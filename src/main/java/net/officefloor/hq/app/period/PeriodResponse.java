package net.officefloor.hq.app.period;

public record PeriodResponse(long id, String name, String from, String to) {
    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.id(), period.name(), period.from().toString(), period.to().toString());
    }
}
