package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A holiday's date (yyyy-mm-dd, required). */
public class ValidateHoliday {

    public void validate(@RequestBody HolidayRequest request, Out<HolidayInput> holiday, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String date = request.date() == null ? "" : request.date().trim();
        if (date.isEmpty()) {
            throw new BadRequestException("Enter a holiday date");
        }
        try {
            holiday.set(new HolidayInput(LocalDate.parse(date)));
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter a date as YYYY-MM-DD");
        }
        changedBy.set(by);
    }
}
