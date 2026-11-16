package net.officefloor.hq.app.period;

import java.util.LinkedHashMap;
import java.util.Map;

/** A period as sent to the screen: its dates as yyyy-MM-dd, whether it is closed or soft-closed and, either, its fixed due-total
 *  and each person's fixed due-total for it, by person id (a person not listed shows 0.00). */
public record PeriodResponse(long id, String sheetId, String name, String from, String to, boolean closed, boolean soft,
        String closedTotal, Map<String, String> personTotals) {
    public static PeriodResponse from(Period period, PeriodRepository periods) {
        Map<String, String> totals = new LinkedHashMap<>();
        if (period.closed() || period.soft()) {
            periods.personTotals(period).forEach((id, total) -> totals.put(String.valueOf(id), total.toPlainString()));
        }
        return new PeriodResponse(period.id(), period.sheetId(), period.name(), period.from().toString(),
                period.to().toString(), period.closed(), period.soft(),
                period.closedTotal() == null ? null : period.closedTotal().toPlainString(), totals);
    }

    /** The period as it stood at the end of a date, in the state it had then (see PeriodRepository#listAsOf). */
    public static PeriodResponse from(Period period, PeriodRepository.StateAsOf state) {
        Map<String, String> totals = new LinkedHashMap<>();
        if (state.closed() || state.soft()) {
            state.personTotals().forEach((id, total) -> totals.put(String.valueOf(id), total.toPlainString()));
        }
        return new PeriodResponse(period.id(), period.sheetId(), period.name(), period.from().toString(),
                period.to().toString(), state.closed(), state.soft(),
                state.closedTotal() == null ? null : state.closedTotal().toPlainString(), totals);
    }
}
