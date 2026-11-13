package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Whether the sheet exists: it has been loaded (or changed). Asking does not load it. */
public class RespondWithExists {
    public void respond(@PathVariable("sheetId") String sheetId, SheetStore store, ObjectResponse<Boolean> response) {
        response.send(store.exists(sheetId.trim()));
    }
}
