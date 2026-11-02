package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with whether the sheet exists, without making it exist. */
public class RespondWithSheetExists {
    public void respond(@PathVariable("sheet") String sheet, SheetRegistry registry,
            ObjectResponse<Boolean> response) {
        response.send(registry.exists(sheet));
    }
}
