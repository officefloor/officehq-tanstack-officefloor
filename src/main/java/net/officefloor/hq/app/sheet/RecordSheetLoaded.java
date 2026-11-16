package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;

/** Notes that the sheet has been loaded, so it exists (a sheet may name it as its parent). */
public class RecordSheetLoaded {
    public void record(@Val Sheet sheet, SheetRepository sheets) {
        sheets.markLoaded(sheet.id());
    }
}
