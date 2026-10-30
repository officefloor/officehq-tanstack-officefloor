package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateHoliday {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody HolidayRequest request, Out<ValidHoliday> body,
            Settings settings, Holidays holidays) throws InvalidHolidayException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String raw = request.date() == null ? "" : request.date().trim();
        LocalDate date;
        try {
            date = LocalDate.parse(raw, DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidHolidayException("The holiday must be a date (YYYY-MM-DD)");
        }
        if (holidays.contains(sheetId, date)) {
            throw new InvalidHolidayException("The date is already a holiday");
        }
        body.set(new ValidHoliday(date, request.user()));
    }
}
