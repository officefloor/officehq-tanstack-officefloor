package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** The calendar year whose quarters are generated: four digits. */
public class ValidateGenerateQuarters {

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateQuartersRequest request,
            Out<ValidQuarters> body, Settings settings)
            throws InvalidPeriodException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String year = request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidPeriodException("The year must be a year (YYYY)");
        }
        body.set(new ValidQuarters(Integer.parseInt(year), request.user()));
    }
}
