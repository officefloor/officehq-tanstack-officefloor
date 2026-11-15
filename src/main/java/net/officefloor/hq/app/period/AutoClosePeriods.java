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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** After the sheet's today is set: with grace days set, closes every open period whose to-date plus
 *  the grace days is before the new today, in order of their to-dates, as a close by hand does, each
 *  recorded as PERIOD_CLOSED with auto=yes. */
public class AutoClosePeriods {
    public void close(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, PeriodRepository periods,
            ClaimRepository claims, ClaimLogRepository log, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets, Audit audit) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        String grace = settings.find(id, SettingRepository.GRACE_DAYS);
        if (grace == null) return;
        LocalDate today = sheets.today(id);
        int days = Integer.parseInt(grace);
        for (Period found : periods.list(id).stream().sorted(Comparator.comparing(Period::to)).toList()) {
            if (found.closed() || !LocalDate.parse(found.to()).plusDays(days).isBefore(today)) continue;
            Period closed = PeriodClosing.closeNow(found, user, periods, claims, log, people, rates, settings, sheets);
            audit.record("PERIOD_CLOSED sheet=" + id + " by=" + user + " period=" + closed.name()
                    + " total=" + closed.closedTotal() + " auto=yes");
        }
    }
}
