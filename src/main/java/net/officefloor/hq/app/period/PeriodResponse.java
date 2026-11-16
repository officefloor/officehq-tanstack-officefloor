package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** A period as sent to the screen: its dates as yyyy-MM-dd, whether it is closed or soft-closed and, either, its fixed due-total
 *  and each person's fixed due-total for it, by person id (a person not listed shows 0.00), and its cap (null for none);
 *  closed or soft-closed, each person's fixed carried figure for it, by person id; and each person's opening balances
 *  pasted for it added up, by person id (added to their live carried figure). */
public record PeriodResponse(long id, String sheetId, String name, String from, String to, boolean closed, boolean soft,
        String closedTotal, Map<String, String> personTotals, Integer cap, Map<String, String> personCarried,
        Map<String, String> openings) {
    public static PeriodResponse from(Period period, PeriodRepository periods) {
        Map<String, String> totals = new LinkedHashMap<>();
        Map<String, String> carried = new LinkedHashMap<>();
        if (period.closed() || period.soft()) {
            periods.personTotals(period).forEach((id, total) -> totals.put(String.valueOf(id), total.toPlainString()));
            periods.personCarried(period).forEach((id, figure) -> carried.put(String.valueOf(id), figure.toPlainString()));
        }
        return new PeriodResponse(period.id(), period.sheetId(), period.name(), period.from().toString(),
                period.to().toString(), period.closed(), period.soft(),
                period.closedTotal() == null ? null : period.closedTotal().toPlainString(), totals, period.cap(), carried,
                openings(periods.openings(period, null)));
    }

    private static Map<String, String> openings(Map<Long, BigDecimal> figures) {
        Map<String, String> out = new LinkedHashMap<>();
        figures.forEach((id, figure) -> out.put(String.valueOf(id), figure.toPlainString()));
        return out;
    }

    /** The period as it stood at the end of a date, in the state it had then (see PeriodRepository#listAsOf). */
    public static PeriodResponse from(Period period, PeriodRepository.StateAsOf state, Map<Long, BigDecimal> openings) {
        Map<String, String> totals = new LinkedHashMap<>();
        Map<String, String> carried = new LinkedHashMap<>();
        if (state.closed() || state.soft()) {
            state.personTotals().forEach((id, total) -> totals.put(String.valueOf(id), total.toPlainString()));
            state.personCarried().forEach((id, figure) -> carried.put(String.valueOf(id), figure.toPlainString()));
        }
        return new PeriodResponse(period.id(), period.sheetId(), period.name(), period.from().toString(),
                period.to().toString(), state.closed(), state.soft(),
                state.closedTotal() == null ? null : state.closedTotal().toPlainString(), totals, period.cap(), carried,
                openings(openings));
    }
}
