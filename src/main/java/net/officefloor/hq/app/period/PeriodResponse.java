package net.officefloor.hq.app.period;

import java.util.Map;
import java.util.TreeMap;

/**
 * A period as the screen shows it: its name, its from and to dates as yyyy-MM-dd, whether it is closed and,
 * when closed, the due-total fixed then (two decimals; null while open) and each person's due-total fixed then, by
 * person id (empty while open), with each person's carried figure fixed then (personCarried), whether it is soft-closed (its figures fixed the same way), and its cap on each
 * person's open claims (null: none), and the opening balances pasted into it, added up by person id
 * (personOpenings), which add to each person's carried figure while it is open, whether it is locked, and where it
 * stands in the order the periods were added (added; higher is later; null where not given).
 */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal,
        Map<String, String> personTotals, boolean soft, Integer cap, Map<String, String> personCarried,
        Map<String, String> personOpenings, boolean locked, Long added) {

    public static PeriodResponse from(Period period) {
        return from(period, Map.of());
    }

    /** The period with the opening balances pasted into it, added up by person id. */
    public static PeriodResponse from(Period period, Map<Long, java.math.BigDecimal> openings) {
        return from(period, openings, null);
    }

    /** The period with its openings and where it stands in the order the periods were added. */
    public static PeriodResponse from(Period period, Map<Long, java.math.BigDecimal> openings, Long added) {
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(), period.closed(),
                period.closedTotal() == null ? null : period.closedTotal().setScale(2).toPlainString(),
                byPerson(period.personTotals()), period.softClosed(), period.cap(), byPerson(period.personCarried()),
                byPerson(openings), period.locked(), added);
    }

    private static Map<String, String> byPerson(Map<Long, java.math.BigDecimal> figures) {
        Map<String, String> totals = new TreeMap<>();
        figures.forEach((id, total) -> totals.put(id.toString(), total.setScale(2).toPlainString()));
        return totals;
    }
}
