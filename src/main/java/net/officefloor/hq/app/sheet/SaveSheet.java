package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;

public class SaveSheet {
    public void save(@Val Sheet sheet, SheetRepository sheets) {
        sheets.saveToday(sheet.id(), sheet.today());
    }
}
