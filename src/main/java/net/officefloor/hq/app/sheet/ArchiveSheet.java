package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Only a steward archives the sheet, and only once. */
public class ArchiveSheet {
    public void archive(@PathVariable("sheet") String sheet, @RequestParam("user") String user, Roles roles,
            SheetArchive archive, Audit audit) throws InvalidArchiveException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidArchiveException("Only a steward archives the sheet");
        }
        if (!archive.archive(sheet, user)) {
            throw new InvalidArchiveException("The sheet is archived already");
        }
        audit.record("SHEET_ARCHIVED sheet=" + sheet + " by=" + user);
    }
}
