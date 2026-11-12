package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClose {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody CloseRequest request, Out<CloseRequest> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        body.set(request == null ? new CloseRequest(null) : request);
    }
}
