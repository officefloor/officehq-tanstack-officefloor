package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Archives the sheet. Only a steward may archive; archiving a sheet already archived is refused. */
public class ArchiveSheet {
    public void archive(@PathVariable("sheetId") String sheetId, @Val ArchiveRequest request, SheetRepository sheets,
            RoleRepository roles, Out<Sheet> sheet) throws InvalidArchiveException {
        String id = sheetId.trim();
        if (!RoleRepository.STEWARD.equals(roles.roleOf(id, request.by()))) {
            throw new InvalidArchiveException("Only a steward may archive the sheet");
        }
        if (!sheets.archive(id, request.by())) {
            throw new InvalidArchiveException("The sheet is already archived");
        }
        sheet.set(sheets.find(id));
    }
}
