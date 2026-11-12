package net.officefloor.hq.app.person;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a transfer by anyone but a steward, or to no sheet or this sheet itself. */
public class ValidateTransfer {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody TransferRequest request, RoleStore roles,
            Out<TransferRequest> body) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        if (roles.find(sheetId.trim(), user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward may transfer people");
        }
        String to = request.to() == null ? "" : request.to().trim();
        if (to.isEmpty()) {
            throw new InvalidSheetException("Give the id of the sheet to transfer to");
        }
        if (to.equals(sheetId.trim())) {
            throw new InvalidSheetException("A person cannot be transferred to their own sheet");
        }
        body.set(new TransferRequest(to, user));
    }
}
