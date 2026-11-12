package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePeriod {
    public void validate(@RequestBody PeriodRequest request, Out<PeriodInput> body) throws InvalidSheetException {
        if (request == null || request.name() == null || request.name().trim().isEmpty()) {
            throw new InvalidSheetException("A name is required");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (to.isBefore(from)) {
            throw new InvalidSheetException("The to date must not be before the from date");
        }
        body.set(new PeriodInput(request.name().trim(), from, to, request.user()));
    }

    private static LocalDate date(String value, String field) throws InvalidSheetException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new InvalidSheetException("A " + field + " date is required");
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
    }
}
