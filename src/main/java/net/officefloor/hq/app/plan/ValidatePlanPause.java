package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a pause or resume by a viewer, on an archived sheet, or that says neither. */
public class ValidatePlanPause {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PlanPauseRequest request,
            RoleStore roles, Out<PlanPauseRequest> body) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        if (request == null || request.on() == null) {
            throw new InvalidSheetException("Say whether to pause or resume the plan");
        }
        body.set(new PlanPauseRequest(request.on(), user));
    }
}
