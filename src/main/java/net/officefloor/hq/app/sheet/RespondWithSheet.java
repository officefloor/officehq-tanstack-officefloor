package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithSheet {
    public void respond(@PathVariable("sheetId") String sheetId, SheetStore store,
            ObjectResponse<SheetResponse> response) {
        String id = sheetId.trim();
        response.send(new SheetResponse(id, store.today(id).toString()));
    }
}
