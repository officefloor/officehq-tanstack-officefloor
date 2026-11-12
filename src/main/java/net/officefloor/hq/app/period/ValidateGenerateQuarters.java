package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Takes the calendar year whose four quarters to generate. */
public class ValidateGenerateQuarters {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateQuartersRequest request,
            Out<QuartersInput> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidSheetException("The year must be a year (YYYY)");
        }
        body.set(new QuartersInput(Integer.parseInt(year), request.user()));
    }
}
