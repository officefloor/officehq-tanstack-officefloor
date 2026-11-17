package net.officefloor.hq.app.sheet;

import java.util.Map;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Whether the sheet exists (see SheetRepository#exists), as {"exists": true|false}. */
public class RespondWithExists {
    public void respond(@PathVariable("sheetId") String sheetId, SheetRepository sheets,
            ObjectResponse<Map<String, Boolean>> response) {
        response.send(Map.of("exists", sheets.exists(sheetId.trim())));
    }
}
