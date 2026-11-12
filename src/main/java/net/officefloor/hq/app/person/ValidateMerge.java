package net.officefloor.hq.app.person;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a merge by anyone but a steward. */
public class ValidateMerge {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody MergeRequest request, RoleStore roles,
            Out<MergeRequest> body) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        if (roles.find(sheetId.trim(), user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward may merge people");
        }
        body.set(request);
    }
}
