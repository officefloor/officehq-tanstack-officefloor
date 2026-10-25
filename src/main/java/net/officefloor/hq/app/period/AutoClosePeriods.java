package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.Comparator;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/**
 * Once the sheet's today is set: with grace days on, closes every open period whose to-date plus the grace days is
 * before the new today, in order of their to-dates, as a close by hand would — whoever set the today, the sheet's
 * close permission governs only a close by hand — each writing PERIOD_CLOSED with auto=yes.
 */
public class AutoClosePeriods {
    public void close(@Val SheetResponse sheet, @Val LocalDate today, @Val ChangedBy by, PeriodRepository repository,
            RoleRepository roles, SettingRepository settings, Rollup rollup, Audit audit) {
        Integer grace = settings.graceDays(sheet.id());
        if (grace == null) {
            return;
        }
        for (Period period : repository.list(sheet.id()).stream()
                .filter(p -> !p.closed() && p.to().plusDays(grace).isBefore(today))
                .sorted(Comparator.comparing(Period::to).thenComparing(Period::id)).toList()) {
            Period closed = ClosePeriod.fix(period, repository, rollup);
            if (closed != null) {
                audit.record(RecordPeriodClosed.line(closed, by) + " auto=yes");
            }
        }
    }
}
