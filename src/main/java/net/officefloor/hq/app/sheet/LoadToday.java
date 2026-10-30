package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadToday {
    public void service(@PathVariable("sheetId") String sheetId, Sheets sheets,
            ObjectResponse<TodayResponse> response) {
        response.send(TodayResponse.of(sheetId, sheets));
    }
}
