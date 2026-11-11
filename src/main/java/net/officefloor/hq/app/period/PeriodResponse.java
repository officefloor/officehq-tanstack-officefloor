package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import net.officefloor.hq.app.setting.Settings;

/**
 * A period, as the screen reads it: its name, its from and to dates (yyyy-MM-dd), whether it is
 * closed, the due-total fixed when it was closed ("" while open), and each person's due-total fixed
 * when it was closed, by person id (empty while open), and whether it is soft-closed (closed, its
 * claims still open to change), and the fiscal year its from-date falls in (FY<yyyy>).
 */
public record PeriodResponse(String name, String from, String to, boolean closed, String closedTotal,
        Map<String, String> personTotals, boolean soft, String fy) {
    public static PeriodResponse from(Period period, Map<Long, BigDecimal> personTotals, Settings settings) {
        Map<String, String> totals = new LinkedHashMap<>();
        personTotals.forEach((id, total) -> totals.put(String.valueOf(id), total.setScale(2).toPlainString()));
        return new PeriodResponse(period.name(), period.from().toString(), period.to().toString(),
                period.closed(), period.closedTotal() == null ? "" : period.closedTotal().setScale(2).toPlainString(),
                totals, period.soft(), settings.fiscalYearOf(period.from()));
    }
}
