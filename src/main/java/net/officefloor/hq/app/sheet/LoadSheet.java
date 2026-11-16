package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Loads a sheet by id. A sheet that was never used starts empty, so every id loads. Read as of a date (see AsOf), it
 *  is the sheet as it stood then. */
public class LoadSheet {
    public void load(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, SheetRepository sheets, Out<Sheet> sheet) {
        sheet.set(sheets.find(sheetId.trim(), AsOf.parse(asOf)));
    }
}
