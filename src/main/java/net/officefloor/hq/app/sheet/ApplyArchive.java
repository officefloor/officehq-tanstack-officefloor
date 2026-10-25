package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** Archives the sheet: only a steward may, and a sheet already archived is refused like any other change. */
public class ApplyArchive {
    public void apply(@Val SheetResponse sheet, @Val ChangedBy by, RoleRepository roles, SheetRepository sheets)
            throws BadRequestException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), by.name()))) {
            throw new BadRequestException("Only a steward may archive the sheet");
        }
        sheets.refuseArchived(sheet.id());
        sheets.archive(sheet.id());
    }
}
