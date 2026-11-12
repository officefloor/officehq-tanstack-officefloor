package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a holiday by a viewer, without a yyyy-mm-dd date, or on a date already on the sheet's list. */
public class ValidateHoliday {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody HolidayRequest request,
            Out<HolidayInput> body, RoleStore roles, HolidayStore holidays) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        String text = request == null || request.date() == null ? "" : request.date().trim();
        if (text.isEmpty()) {
            throw new InvalidSheetException("A date is required");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("The date must be a date (yyyy-mm-dd)");
        }
        if (holidays.has(sheet, date)) {
            throw new InvalidSheetException(date + " is already a holiday");
        }
        body.set(new HolidayInput(date, user));
    }
}
