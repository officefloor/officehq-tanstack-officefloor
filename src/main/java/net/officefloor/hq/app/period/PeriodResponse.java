package net.officefloor.hq.app.period;

public record PeriodResponse(long id, String name, String from, String to, boolean closed, String closedTotal) {
    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.id(), period.name(), period.from().toString(), period.to().toString(),
                period.closed(), period.closedTotal() == null ? null : period.closedTotal().toPlainString());
    }
}
