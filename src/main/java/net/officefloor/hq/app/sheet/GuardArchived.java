package net.officefloor.hq.app.sheet;

import org.springframework.web.bind.annotation.PathVariable;

/** The step after GuardViewer on every change: refuses it, before anything is read or written, when the sheet is
 *  archived (an archived sheet shows everything and changes nothing). */
public class GuardArchived {
    public void guard(@PathVariable("sheetId") String sheetId, SheetRepository repository)
            throws SheetArchivedException {
        if (repository.findById(sheetId.trim()).map(Sheet::isArchived).orElse(false)) {
            throw new SheetArchivedException("The sheet is archived: nothing on it can be changed.");
        }
    }
}
