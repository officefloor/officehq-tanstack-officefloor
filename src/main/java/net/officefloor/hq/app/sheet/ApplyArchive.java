package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Archives the sheet. Only a steward may (one already archived is refused before, by GuardArchived). */
public class ApplyArchive {
    public void apply(@Val Sheet sheet, @RequestHeader(name = "X-User", required = false) String user,
            RoleRepository roles, SheetRepository repository) throws SheetArchivedException {
        if (!Role.STEWARD.equals(roles.roleOf(sheet.getId(), AuditUser.decode(user)))) {
            throw new SheetArchivedException("Only a steward can archive the sheet.");
        }
        sheet.setArchived(true);
        repository.save(sheet);
    }
}
