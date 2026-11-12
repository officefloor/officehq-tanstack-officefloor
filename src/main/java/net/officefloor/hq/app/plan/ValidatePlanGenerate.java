package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a generate by a viewer or on an archived sheet. */
public class ValidatePlanGenerate {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PlanGenerateRequest request,
            Out<PlanGenerateRequest> body, RoleStore roles) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        body.set(new PlanGenerateRequest(user));
    }
}
