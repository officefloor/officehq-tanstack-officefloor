package net.officefloor.hq.app.period;

import java.util.Comparator;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Once the sheet's new today is set, closes every open period whose to-date plus the sheet's grace days is before it
 * (none while the grace days are off), in order of their to-dates, as a close by hand would; each is recorded as
 * PERIOD_CLOSED with auto=yes.
 */
public class AutoClosePeriods {
    public void close(@Val Sheet sheet, PeriodRepository periods, PeriodCloser closer, SettingRepository settings,
            @RequestParam("user") String user, Audit audit) {
        Integer graceDays = settings.graceDays(sheet.id());
        if (graceDays == null) {
            return;
        }
        for (Period period : periods.findAll(sheet.id()).stream().sorted(Comparator.comparing(Period::to)).toList()) {
            if (period.fixed() || !period.to().plusDays(graceDays).isBefore(sheet.today())) {
                continue;
            }
            Period closed = closer.close(period);
            periods.saveClosed(closed);
            audit.record("PERIOD_CLOSED sheet=" + closed.sheetId() + " by=" + user + " period=" + closed.name()
                    + " total=" + closed.closedTotal().setScale(2).toPlainString() + " auto=yes");
        }
    }
}
