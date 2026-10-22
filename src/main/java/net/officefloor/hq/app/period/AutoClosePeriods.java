package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.Comparator;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.TodayRequest;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Once the sheet's new today is set: while it has grace days, close each open period (not a soft-closed one) whose to-date plus them is
 *  before the new today, in order of their to-dates, exactly as a close by hand would, each recorded as
 *  PERIOD_CLOSED with auto=yes at its end. */
public class AutoClosePeriods {
    public void close(@PathVariable("sheetId") String sheetId, @Val TodayRequest request, @Val LocalDate today,
            PeriodRepository periods, ClaimRepository claims, ClaimLogRepository logs, PersonRepository people,
            RateRepository rates, SettingRepository settings, SheetRepository sheets, Audit audit)
            throws InvalidPeriodException {
        String id = sheetId.trim();
        Integer grace = settings.graceDays(id);
        if (grace == null) {
            return;
        }
        periods.lockSheet(id);
        var due = periods.list(id).stream()
                .filter(p -> !p.closed() && !p.soft() && p.to().plusDays(grace).isBefore(today))
                .sorted(Comparator.comparing(Period::to))
                .toList();
        for (Period period : due) {
            Period closed = PeriodCloser.fix(id, period, periods, claims, logs, people, rates, settings, sheets);
            audit.record("PERIOD_CLOSED sheet=" + closed.sheetId() + " by=" + request.user()
                    + " period=" + closed.name() + " total=" + closed.closedTotal().toPlainString() + " auto=yes");
        }
    }
}
