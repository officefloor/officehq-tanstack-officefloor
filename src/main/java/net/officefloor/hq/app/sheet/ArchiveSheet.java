package net.officefloor.hq.app.sheet;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Archives the sheet. Only a steward may; a sheet already archived is refused. */
public class ArchiveSheet {
    public void archive(@Val Sheet sheet, SheetRepository sheets, RoleRepository roles, HttpServletRequest request,
            Out<Sheet> archived) throws StewardOnlyException, SheetArchivedException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), Audit.user(request)))) {
            throw new StewardOnlyException("Only a steward may archive the sheet");
        }
        if (!sheets.archive(sheet.id())) throw new SheetArchivedException(sheet.id());
        archived.set(new Sheet(sheet.id(), sheet.today(), true));
    }
}
