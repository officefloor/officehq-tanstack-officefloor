package net.officefloor.hq.app.holiday;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a holiday whose date is not yyyy-mm-dd or is already on the sheet's list. */
public class ValidateHolidayRequest {
    public void validate(@Val Sheet sheet, @RequestBody @Valid HolidayRequest request, HolidayRepository holidays,
            Out<Holiday> holiday) throws InvalidHolidayException {
        String text = request.date() == null ? "" : request.date().trim();
        LocalDate date;
        try {
            date = LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidHolidayException("A holiday's date must be yyyy-mm-dd.");
        }
        if (holidays.exists(sheet.id(), date)) {
            throw new InvalidHolidayException(date + " is already a holiday.");
        }
        holiday.set(new Holiday(sheet.id(), date));
    }
}
