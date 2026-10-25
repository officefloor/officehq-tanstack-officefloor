package net.officefloor.hq.app.sheet;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Whether the store holds the sheet; unlike loading it, never starts one. */
public class RespondWithSheetExists {
    public void respond(@PathVariable("sheetId") String sheetId, SheetRepository repository,
            ObjectResponse<Boolean> response) {
        response.send(repository.existsById(sheetId.trim()));
    }
}
