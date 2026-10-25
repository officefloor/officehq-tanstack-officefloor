package net.officefloor.hq.app.access;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/** Refuses any change by a viewer on the sheet, or to an archived sheet, before it touches the store. */
public class RefuseViewer {
    public void refuse(@Val SheetResponse sheet, @Val ChangedBy by, RoleRepository roles,
            SheetRepository sheets) throws BadRequestException {
        if (roles.isViewer(sheet.id(), by.name())) {
            throw new BadRequestException("A viewer may not change anything on this sheet");
        }
        sheets.refuseArchived(sheet.id());
    }
}
