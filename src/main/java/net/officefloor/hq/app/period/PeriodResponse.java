package net.officefloor.hq.app.period;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A period as the screen sees it: the dates as yyyy-mm-dd, the due-total fixed when closed (null while open), and
 * each person's due-total for it fixed then, by person id (empty while open; a person added later has none), and
 * whether it is soft-closed (its figures fixed so, its claims still open to change), and its claim cap (null: none).
 */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal,
        Map<Long, String> personTotals, boolean soft, Integer cap) {

    public static PeriodResponse from(Period period) {
        Map<Long, String> totals = new LinkedHashMap<>();
        period.personTotals().forEach((id, total) -> totals.put(id, total.toPlainString()));
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(), period.closed(),
                period.closedTotal() == null ? null : period.closedTotal().toPlainString(), totals, period.soft(), period.cap());
    }
}
