package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePeriod {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PeriodRequest request, Out<PeriodInput> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        if (request == null || request.name() == null || request.name().trim().isEmpty()) {
            throw new InvalidSheetException("A name is required");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (to.isBefore(from)) {
            throw new InvalidSheetException("The to date must not be before the from date");
        }
        body.set(new PeriodInput(request.name().trim(), from, to, request.user(), cap(request.cap())));
    }

    private static Integer cap(String value) throws InvalidSheetException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (!text.matches("\\d{1,6}") || Integer.parseInt(text) < 1) {
            throw new InvalidSheetException("The cap must be a whole number of at least 1: " + text);
        }
        return Integer.parseInt(text);
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
