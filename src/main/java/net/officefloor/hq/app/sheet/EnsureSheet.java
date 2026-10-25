package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;

public class EnsureSheet {
    public void ensure(@Val SheetResponse sheet, SheetRepository repository) {
        repository.ensure(sheet.id());
    }
}
