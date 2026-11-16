package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads a sheet by id. A sheet that was never used starts empty, so every id loads. */
public class LoadSheet {
    public void load(@PathVariable("sheetId") String sheetId, SheetRepository sheets, Out<Sheet> sheet) {
        sheet.set(sheets.find(sheetId.trim()));
    }
}
