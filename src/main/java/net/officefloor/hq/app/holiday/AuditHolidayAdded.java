package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a holiday added: HOLIDAY_ADDED sheet=<id> by=<user> date=<date>, then how the due-by dates it
 *  moved moved the sheet's figures. */
public class AuditHolidayAdded {
    public void audit(@PathVariable("sheetId") String sheetId, @Val HolidayInput holiday, @Val Rollup before,
            Audit audit, RollupService rollups) {
        audit.record("HOLIDAY_ADDED sheet=" + sheetId.trim() + " by=" + holiday.user() + " date=" + holiday.date());
        rollups.record(sheetId.trim(), holiday.user(), before);
    }
}
