package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.TodayBy;
import net.officefloor.plugin.variable.Val;

/** Once the sheet's new today is stored, closes every open period (not closed nor soft-closed) whose to-date plus the sheet's grace days is before
 *  it (none while the grace days are blank), in order of their to-dates, exactly as a close by hand would, each
 *  writing PERIOD_CLOSED with auto=yes at its end. */
public class AutoClosePeriods {
    public void close(@Val Sheet sheet, @Val TodayBy by, SettingRepository settings, PeriodRepository periods,
            PeriodCloser closer, Audit audit) throws InvalidPeriodException {
        Integer grace = settings.graceDays(sheet.id());
        if (grace == null) {
            return;
        }
        List<Period> due = periods.list(sheet.id()).stream()
                .filter((p) -> !p.closed() && !p.soft() && p.to().plusDays(grace).isBefore(sheet.today()))
                .sorted(Comparator.comparing(Period::to))
                .toList();
        for (Period period : due) {
            BigDecimal total = closer.close(period);
            audit.record("PERIOD_CLOSED sheet=" + period.sheetId() + " by=" + by.by() + " period=" + period.name()
                    + " total=" + total.toPlainString() + " auto=yes");
        }
    }
}
