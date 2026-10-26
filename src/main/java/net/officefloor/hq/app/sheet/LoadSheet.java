package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadSheet {
    // A sheet that was never loaded before starts empty, so any id loads.
    public void load(@PathVariable("sheetId") String sheetId, Sheets sheets, Out<Sheet> sheetOut) {
        sheetOut.set(sheets.find(sheetId.trim()));
    }
}
