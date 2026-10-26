package net.officefloor.hq.app.period;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A period as the screen sees it: the dates as yyyy-mm-dd, the due-total fixed when closed (null while open), and
 * each person's due-total for it fixed then, by person id (empty while open; a person added later has none).
 */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal,
        Map<Long, String> personTotals) {

    public static PeriodResponse from(Period period) {
        Map<Long, String> totals = new LinkedHashMap<>();
        period.personTotals().forEach((id, total) -> totals.put(id, total.toPlainString()));
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(), period.closed(),
                period.closedTotal() == null ? null : period.closedTotal().toPlainString(), totals);
    }
}
