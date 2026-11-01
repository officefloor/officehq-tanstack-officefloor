package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSheetExists {
    public void respond(@PathVariable("sheetId") String sheetId, Sheets sheets, ObjectResponse<Boolean> response) {
        response.send(sheets.exists(sheetId));
    }
}
