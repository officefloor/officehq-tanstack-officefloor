package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the hold form: a blank from-date takes the sheet's today, a blank to-date leaves the hold open-ended. */
public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<HoldInput> hold) throws BadRequestException {
        LocalDate from = date(request == null ? null : request.from(), "from");
        LocalDate to = date(request == null ? null : request.to(), "to");
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("to must not be before from");
        }
        hold.set(new HoldInput(from, to));
    }

    private static LocalDate date(String value, String field) throws BadRequestException {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) return null;
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(field + " must be yyyy-mm-dd");
        }
    }
}
