package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Whether the sheet exists (it has been loaded); asking does not start it. */
public class LoadExists {
    public void service(@PathVariable("sheetId") String sheetId, Sheets sheets,
            ObjectResponse<ExistsResponse> response) {
        response.send(new ExistsResponse(sheets.exists(sheetId)));
    }
}
