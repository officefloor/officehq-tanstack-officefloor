package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSheet {
    public void respond(@PathVariable("sheetId") String sheetId, ObjectResponse<SheetResponse> response) {
        response.send(new SheetResponse(sheetId.trim()));
    }
}
