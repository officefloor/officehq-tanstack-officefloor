package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;

public class MarkSheetLoaded {
    public void mark(@Val Sheet sheet, Sheets sheets) {
        sheets.markLoaded(sheet.id());
    }
}
