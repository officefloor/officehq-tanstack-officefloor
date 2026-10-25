package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Val;

/** Refuses any change to an archived sheet, before it touches the store. */
public class RefuseArchived {
    public void refuse(@Val SheetResponse sheet, SheetRepository sheets) throws BadRequestException {
        sheets.refuseArchived(sheet.id());
    }
}
