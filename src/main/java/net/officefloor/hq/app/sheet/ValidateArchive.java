package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses archiving by anyone but a steward, and archiving a sheet already archived. */
public class ValidateArchive {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ArchiveRequest request,
            Out<ArchiveRequest> body, RoleStore roles, SheetStore sheets) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (roles.find(sheet, user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward can archive the sheet");
        }
        sheets.refuseArchived(sheet);
        body.set(new ArchiveRequest(user));
    }
}
