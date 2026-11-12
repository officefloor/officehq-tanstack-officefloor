package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a generate on an archived sheet. A viewer's generate is answered by GeneratePlanClaims, making none. */
public class ValidatePlanGenerate {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PlanGenerateRequest request,
            Out<PlanGenerateRequest> body, SheetStore sheets) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        sheets.refuseArchived(sheetId.trim());
        body.set(new PlanGenerateRequest(user));
    }
}
