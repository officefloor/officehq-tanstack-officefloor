package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Sets the sheet's today; a new today closes every open claim editor, releasing its lock. */
public class ApplyToday {
    public void apply(@PathVariable("sheetId") String sheetId, @Val LocalDate today, Sheets sheets, Claims claims) {
        if (!today.equals(sheets.today(sheetId))) claims.unlockAll(sheetId);
        sheets.setToday(sheetId, today);
    }
}
