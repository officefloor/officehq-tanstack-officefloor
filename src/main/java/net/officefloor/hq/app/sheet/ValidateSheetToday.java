package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The sheet's new today. Publishes it with the user making the change. */
public class ValidateSheetToday {
    public void validate(@RequestBody SheetTodayRequest request, Out<LocalDate> today, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String text = request == null || request.today() == null ? "" : request.today().trim();
        try {
            today.set(LocalDate.parse(text));
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter a date as yyyy-mm-dd");
        }
        changedBy.set(by);
    }
}
