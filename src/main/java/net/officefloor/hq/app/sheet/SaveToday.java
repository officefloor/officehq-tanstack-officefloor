package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveToday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate today, Sheets sheets,
            ObjectResponse<TodayResponse> response) {
        sheets.setToday(sheetId, today);
        response.send(new TodayResponse(sheetId, today.toString()));
    }
}
