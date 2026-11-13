package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodClosing;
import net.officefloor.hq.app.period.PeriodResponse;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.pool.PoolCuts;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** With grace days set, closes every open period (not a soft-closed one, whose figures stay fixed) whose
 *  to-date plus the grace days is before the new today, in order of their to-dates, exactly as a close by hand would, each recorded as
 *  PERIOD_CLOSED sheet=<id> by=<user> period=<name> total=<0.00> auto=yes. */
public class AutoClosePeriods {
    public void close(@PathVariable("sheetId") String sheetId, @Val TodayRequest request, @Val LocalDate today,
            SettingStore settings, PeriodStore periods, RollupService rollups, PoolCuts cuts, Audit audit)
            throws InvalidSheetException {
        String sheet = sheetId.trim();
        Integer grace = settings.graceDays(sheet);
        if (grace == null) {
            return;
        }
        List<PeriodResponse> due = periods.list(sheet).stream()
                .filter(p -> !p.closed() && !p.soft() && LocalDate.parse(p.to()).plusDays(grace).isBefore(today))
                .sorted(Comparator.comparing(PeriodResponse::to)).toList();
        for (PeriodResponse period : due) {
            PeriodResponse closed = PeriodClosing.fix(sheet, period, periods, rollups, cuts);
            audit.record("PERIOD_CLOSED sheet=" + sheet + " by=" + request.user() + " period=" + closed.name()
                    + " total=" + closed.total() + " auto=yes");
        }
    }
}
