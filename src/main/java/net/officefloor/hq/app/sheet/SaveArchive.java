package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Archive the sheet when a steward asks and it is not archived yet, recording SHEET_ARCHIVED. */
public class SaveArchive {
    public void save(@PathVariable("sheetId") String sheetId, @Val ArchiveRequest request, SheetRepository sheets,
            RoleRepository roles, Audit audit) throws InvalidSheetException {
        String sheet = sheetId.trim();
        if (!Role.STEWARD.equals(roles.of(sheet, request.user()))) {
            throw new InvalidSheetException("Only a steward can archive the sheet.");
        }
        if (!sheets.archive(sheet)) {
            throw new InvalidSheetException("This sheet is already archived.");
        }
        audit.record("SHEET_ARCHIVED sheet=" + sheet + " by=" + request.user());
    }
}
