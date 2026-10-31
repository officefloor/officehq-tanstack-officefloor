package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Runs once the sheet's new today is set: while the sheet has grace days, every open or soft-closed period whose
 *  to-date plus them is before the new today is closed, in order of their to-dates, exactly as a
 *  close by hand would, each writing its PERIOD_CLOSED line ending auto=yes. */
public class AutoClosePeriods {
    public void close(@PathVariable("sheetId") String sheetId, @Val LocalDate today, @Val Actor actor,
            Settings settings, Periods periods, PeriodCloser closer, Audit audit) {
        Integer grace = settings.graceDays(sheetId);
        if (grace == null) return;
        List<Period> due = periods.onSheet(sheetId).stream()
                .filter(p -> !p.hardClosed() && p.to().plusDays(grace).isBefore(today))
                .sorted(Comparator.comparing(Period::to))
                .toList();
        for (Period open : due) {
            Period period = closer.close(periods.find(sheetId, open.name()));
            audit.record("PERIOD_CLOSED sheet=" + sheetId + " by=" + actor.user() + " period=" + period.name()
                    + " total=" + period.closedTotal().setScale(2).toPlainString() + " auto=yes");
        }
    }
}
