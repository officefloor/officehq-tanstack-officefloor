package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a claim hold toggle by a user who may not change the sheet. */
public class ValidateHoldClaim {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody HoldClaimRequest request,
            Out<HoldClaimInput> body, RoleStore roles) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        body.set(new HoldClaimInput(user));
    }
}
