package net.officefloor.hq.app.sheet;

import org.springframework.web.bind.annotation.PathVariable;

public class SaveArchive {
    public void save(@PathVariable("sheetId") String sheetId, SheetStore store) {
        store.archive(sheetId.trim());
    }
}
