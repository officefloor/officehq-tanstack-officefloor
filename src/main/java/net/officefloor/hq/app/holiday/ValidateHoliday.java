package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.setting.InvalidSettingException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for a date that is not one, and for a date already on the sheet's list. */
public class ValidateHoliday {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody HolidayRequest request,
            HolidayRepository holidays, Out<HolidayRequest> body) throws InvalidSettingException {
        String text = request == null || request.date() == null ? "" : request.date().trim();
        LocalDate date;
        try {
            date = LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSettingException("Not a date: " + text);
        }
        if (holidays.contains(sheetId.trim(), date)) {
            throw new InvalidSettingException("Already a holiday: " + date);
        }
        body.set(new HolidayRequest(date.toString()));
    }
}
