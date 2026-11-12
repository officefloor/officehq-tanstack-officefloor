package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a waiver by anyone but a steward. */
public class ValidateWaive {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody WaiveRequest request, Out<WaiveInput> body,
            RoleStore roles) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        if (roles.find(sheetId.trim(), user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward can waive a late charge");
        }
        body.set(new WaiveInput(user));
    }
}
