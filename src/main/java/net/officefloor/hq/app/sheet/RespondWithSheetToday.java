package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSheetToday {
    public void respond(@PathVariable("sheetId") String sheetId, SheetClock clock,
            ObjectResponse<SheetToday> response) {
        response.send(new SheetToday(clock.today(sheetId).toString()));
    }
}
