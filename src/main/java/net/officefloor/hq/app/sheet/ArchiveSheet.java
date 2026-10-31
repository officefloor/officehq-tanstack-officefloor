package net.officefloor.hq.app.sheet;

import org.springframework.web.bind.annotation.PathVariable;

public class ArchiveSheet {
    public void archive(@PathVariable("sheetId") String sheetId, Sheets sheets) {
        sheets.archive(sheetId);
    }
}
