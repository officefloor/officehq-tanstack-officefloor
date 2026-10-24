package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

/** A period as the screen shows it: its name, its from and to date (YYYY-MM-DD), whether it is closed, whether it is soft-closed, and the
 *  due-total fixed when it was closed (null while open), and each person's fixed then, by person id (empty while open), and its cap on open claims per person (null for none), and each person's carried figure fixed when it was closed, by person id (empty while open). */
public record PeriodResponse(long id, String name, String from, String to, boolean closed, boolean softClosed, String closedTotal,
        Map<String, String> personTotals, Integer cap, Map<String, String> personCarried) {

    public static PeriodResponse from(Period period) {
        return new PeriodResponse(period.getId(), period.getName(), period.getFromDate().toString(),
                period.getToDate().toString(), period.isClosed(), period.isSoftClosed(),
                period.getClosedTotal() == null ? null : period.getClosedTotal().toPlainString(),
                byPerson(period.getPersonTotals()), period.getCap(), byPerson(period.getPersonCarried()));
    }

    private static Map<String, String> byPerson(Map<Long, BigDecimal> fixed) {
        Map<String, String> totals = new TreeMap<>();
        fixed.forEach((id, total) -> totals.put(String.valueOf(id), total.setScale(2).toPlainString()));
        return totals;
    }
}
