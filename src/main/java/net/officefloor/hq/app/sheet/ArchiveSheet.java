package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class ArchiveSheet {
    public void archive(@PathVariable("sheetId") String sheetId, Sheets sheets, Out<Sheet> sheetOut) {
        Sheet sheet = sheets.find(sheetId.trim()).archive();
        sheets.save(sheet);
        sheetOut.set(sheet);
    }
}
