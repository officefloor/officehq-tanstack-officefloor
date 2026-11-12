package net.officefloor.hq.app.person;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePatron {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PatronRequest request, Out<PatronRequest> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseViewer(sheetId.trim(), request == null ? null : request.user());
        body.set(request == null ? new PatronRequest(null, null) : request);
    }
}
