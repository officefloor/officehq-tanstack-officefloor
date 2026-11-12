package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a clone by anyone but a steward, or to no sheet, or to a sheet that exists (has been used). */
public class ValidateClone {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody CloneRequest request,
            Out<CloneRequest> body, RoleStore roles, SheetStore sheets) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (roles.find(sheet, user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward can clone the sheet");
        }
        String to = request.to() == null ? "" : request.to().trim();
        if (to.isEmpty()) {
            throw new InvalidSheetException("Give the id of the sheet to clone to");
        }
        if (to.equals(sheet) || sheets.exists(to)) {
            throw new InvalidSheetException("The sheet " + to + " is already in use");
        }
        body.set(new CloneRequest(to, user));
    }
}
