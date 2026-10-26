package net.officefloor.hq.app.period;

import java.util.LinkedHashMap;
import java.util.Map;

/** A period; once closed (or soft-closed: `soft`), `personTotals` holds each person's due-total fixed then, by person id; `cap` caps each person's OPEN or PENDING claims in it (null: none); `personCarried` each person's carried figure fixed then (empty while open). */
public record PeriodResponse(long id, String name, String from, String to, boolean closed, String closedTotal,
        Map<String, String> personTotals, boolean soft, Integer cap, Map<String, String> personCarried) {
    public static PeriodResponse from(Period period) {
        Map<String, String> personTotals = new LinkedHashMap<>();
        period.personTotals().forEach((id, total) -> personTotals.put(String.valueOf(id), total.toPlainString()));
        Map<String, String> personCarried = new LinkedHashMap<>();
        period.personCarried().forEach((id, carried) -> personCarried.put(String.valueOf(id), carried.toPlainString()));
        return new PeriodResponse(period.id(), period.name(), period.from().toString(), period.to().toString(),
                period.closed(), period.closedTotal() == null ? null : period.closedTotal().toPlainString(),
                personTotals, period.soft(), period.cap(), personCarried);
    }
}
