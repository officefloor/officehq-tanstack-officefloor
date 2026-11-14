package net.officefloor.hq.app.period;

import java.util.Map;
import java.util.TreeMap;

/**
 * A period as the screen shows it: its name, its from and to dates as yyyy-MM-dd, whether it is closed and,
 * when closed, the due-total fixed then (two decimals; null while open) and each person's due-total fixed then, by
 * person id (empty while open), whether it is soft-closed (its figures fixed the same way), and its cap on each
 * person's open claims (null: none).
 */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal,
        Map<String, String> personTotals, boolean soft, Integer cap) {

    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(), period.closed(),
                period.closedTotal() == null ? null : period.closedTotal().setScale(2).toPlainString(),
                personTotals(period), period.softClosed(), period.cap());
    }

    private static Map<String, String> personTotals(Period period) {
        Map<String, String> totals = new TreeMap<>();
        period.personTotals().forEach((id, total) -> totals.put(id.toString(), total.setScale(2).toPlainString()));
        return totals;
    }
}
