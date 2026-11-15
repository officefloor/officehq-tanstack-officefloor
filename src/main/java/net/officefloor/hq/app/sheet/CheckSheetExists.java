package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Whether a sheet with the id was ever loaded; reading it never makes it. */
public class CheckSheetExists {
    public void check(@PathVariable("sheetId") String sheetId, SheetRepository sheets,
            ObjectResponse<Boolean> response) {
        response.send(sheets.find(sheetId.trim()) != null);
    }
}
