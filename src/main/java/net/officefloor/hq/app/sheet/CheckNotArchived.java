package net.officefloor.hq.app.sheet;

import org.springframework.web.bind.annotation.PathVariable;

/** Refuses (400) any change to an archived sheet: it runs first, so a refused change writes nothing. */
public class CheckNotArchived {
    public void check(@PathVariable("sheetId") String sheetId, SheetRepository sheets) throws SheetArchivedException {
        if (sheets.isArchived(sheetId)) {
            throw new SheetArchivedException("An archived sheet cannot be changed.");
        }
    }
}
