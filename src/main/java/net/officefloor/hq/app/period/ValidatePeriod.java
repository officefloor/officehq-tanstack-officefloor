package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePeriod {

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PeriodRequest request,
            Out<Period> period) throws InvalidPeriodException {
        String name = request == null || request.name() == null ? "" : request.name().strip();
        if (name.isEmpty()) {
            throw new InvalidPeriodException("A name is required.");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (from.isAfter(to)) {
            throw new InvalidPeriodException("The from-date must not be after the to-date.");
        }
        period.set(new Period(sheetId, name, from, to));
    }

    private static LocalDate date(String value, String which) throws InvalidPeriodException {
        String text = value == null ? "" : value.strip();
        if (text.isEmpty()) {
            throw new InvalidPeriodException("A " + which + "-date is required.");
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a date (yyyy-MM-dd).");
        }
    }
}
