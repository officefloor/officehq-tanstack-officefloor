package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Once the sheet's today is changed: closes every open period whose to-date plus the sheet's grace
 * days is before the new today, in order of their to-dates, exactly as a close by hand would, each
 * recording PERIOD_CLOSED with auto=yes as it closes. Nothing closes while the grace days are off.
 */
public class AutoClosePeriods {
    public void close(@Val Sheet sheet, @Val LocalDate today, PeriodRepository periods, PersonRepository people,
            ClaimRepository claims, ClaimLogRepository log, RateRepository rates, SettingsRepository settings,
            HttpServletRequest request, Audit audit) {
        Settings sheetSettings = settings.find(sheet.id());
        Sheet now = new Sheet(sheet.id(), today, sheet.archived());
        List<Period> due = periods.findAll(sheet.id()).stream()
                .filter((p) -> !p.closed() && sheetSettings.pastGrace(p.to(), today))
                .sorted(Comparator.comparing(Period::to)).toList();
        for (Period period : due) {
            Period closed = ClosePeriod.fix(now, period, false, periods, people, claims, log, rates, settings);
            if (closed == null) continue; // another request closed it first
            audit.record("PERIOD_CLOSED sheet=" + sheet.id() + " by=" + Audit.user(request)
                    + " period=" + closed.name() + " total=" + closed.closedTotal().setScale(2).toPlainString()
                    + " auto=yes");
        }
    }
}
