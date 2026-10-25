package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A hold's from-date (blank for the sheet's today) and to-date (blank: open-ended). Publishes them parsed, with the user making the change. */
public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<HoldInput> hold, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        LocalDate from = date(request == null ? null : request.from());
        LocalDate to = date(request == null ? null : request.to());
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("A hold's to-date cannot be before its from-date");
        }
        hold.set(new HoldInput(from, to));
        changedBy.set(by);
    }

    private static LocalDate date(String value) throws BadRequestException {
        String date = value == null ? "" : value.trim();
        if (date.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Enter a date as YYYY-MM-DD");
        }
    }
}
