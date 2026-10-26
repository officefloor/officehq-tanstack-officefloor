package net.officefloor.hq.app.sheet;

import org.springframework.web.bind.annotation.PathVariable;

public class RefuseArchived {
    // An archived sheet keeps everything visible and changes nothing: every write is refused before anything is read, stored or recorded.
    public void refuse(@PathVariable("sheetId") String sheetId, Sheets sheets) throws SheetArchivedException {
        if (sheets.isArchived(sheetId.trim())) {
            throw new SheetArchivedException("The sheet is archived: it can change nothing.");
        }
    }
}
