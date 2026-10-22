package net.officefloor.hq.app.sheet;

import org.springframework.web.bind.annotation.PathVariable;

/** Fails when the sheet id has never been loaded. */
public class LoadSheet {
    public void load(@PathVariable("sheetId") String sheetId, SheetRepository repository)
            throws SheetNotFoundException {
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.exists(id)) {
            throw new SheetNotFoundException(id);
        }
    }
}
