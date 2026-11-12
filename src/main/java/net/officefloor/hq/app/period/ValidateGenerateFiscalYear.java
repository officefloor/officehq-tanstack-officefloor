package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Turns a fiscal year into the range of its twelve months: from the sheet's fiscal start month in the year. */
public class ValidateGenerateFiscalYear {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateFiscalYearRequest request,
            SettingStore settings, Out<GenerateInput> body, RoleStore roles) throws InvalidSheetException {
        String sheet = sheetId.trim();
        roles.refuseChange(sheet, request == null ? null : request.user());
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidSheetException("The fiscal year must be a year (YYYY)");
        }
        YearMonth from = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(sheet));
        body.set(new GenerateInput(from, from.plusMonths(11), request.user()));
    }
}
