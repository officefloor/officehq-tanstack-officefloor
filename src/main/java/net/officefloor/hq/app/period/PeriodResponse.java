package net.officefloor.hq.app.period;

import java.util.LinkedHashMap;
import java.util.Map;

/** A period; once closed, `personTotals` holds each person's due-total fixed then, by person id. */
public record PeriodResponse(long id, String name, String from, String to, boolean closed, String closedTotal,
        Map<String, String> personTotals) {
    public static PeriodResponse from(Period period) {
        Map<String, String> personTotals = new LinkedHashMap<>();
        period.personTotals().forEach((id, total) -> personTotals.put(String.valueOf(id), total.toPlainString()));
        return new PeriodResponse(period.id(), period.name(), period.from().toString(), period.to().toString(),
                period.closed(), period.closedTotal() == null ? null : period.closedTotal().toPlainString(),
                personTotals);
    }
}
