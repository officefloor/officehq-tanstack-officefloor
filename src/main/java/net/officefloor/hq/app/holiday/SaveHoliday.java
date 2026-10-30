package net.officefloor.hq.app.holiday;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the holiday; the due-by dates already fixed stay as they are. */
public class SaveHoliday {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidHoliday holiday, Holidays holidays, Audit audit,
            ObjectResponse<List<String>> response) {
        holidays.add(sheetId, holiday.date());
        audit.recordOnCommit("HOLIDAY_ADDED sheet=" + sheetId + " by=" + holiday.user() + " date=" + holiday.date());
        response.send(holidays.list(sheetId));
    }
}
