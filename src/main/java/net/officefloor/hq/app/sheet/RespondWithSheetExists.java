package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSheetExists {
    // Whether the sheet exists (see Sheets#exists); asking does not mark it loaded.
    public void respond(@PathVariable("sheetId") String sheetId, Sheets sheets, ObjectResponse<Boolean> response) {
        response.send(sheets.exists(sheetId.trim()));
    }
}
