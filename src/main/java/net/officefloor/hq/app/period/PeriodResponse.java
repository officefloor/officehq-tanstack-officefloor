package net.officefloor.hq.app.period;

import java.util.Map;
import java.util.TreeMap;

/** A period as the screen shows it: its name, its from and to date (YYYY-MM-DD), whether it is closed and the
 *  due-total fixed when it was closed (null while open), and each person's fixed then, by person id (empty while open). */
public record PeriodResponse(long id, String name, String from, String to, boolean closed, String closedTotal,
        Map<String, String> personTotals) {

    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.getId(), period.getName(), period.getFromDate().toString(),
                period.getToDate().toString(), period.isClosed(),
                period.getClosedTotal() == null ? null : period.getClosedTotal().toPlainString(),
                personTotalsOf(period));
    }

    private static Map<String, String> personTotalsOf(Period period) {
        Map<String, String> totals = new TreeMap<>();
        period.getPersonTotals().forEach((id, total) -> totals.put(String.valueOf(id), total.setScale(2).toPlainString()));
        return totals;
    }
}
