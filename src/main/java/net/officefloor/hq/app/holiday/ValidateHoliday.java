package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateHoliday {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // The date must be a real date, and not already on the sheet's list.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody HolidayRequest body, Holidays holidays,
            Out<Holiday> holiday) throws HolidayRefusedException {
        String sheet = sheetId.trim();
        String raw = body == null || body.date() == null ? "" : body.date().trim();
        LocalDate date;
        try {
            date = LocalDate.parse(raw, ISO);
        } catch (DateTimeParseException e) {
            throw new HolidayRefusedException("The holiday must be a date (yyyy-mm-dd).");
        }
        if (holidays.has(sheet, date)) {
            throw new HolidayRefusedException(date + " is already a holiday.");
        }
        holiday.set(new Holiday(sheet, date));
    }
}
