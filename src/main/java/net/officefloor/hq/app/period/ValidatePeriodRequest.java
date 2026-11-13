package net.officefloor.hq.app.period;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePeriodRequest {

    public void validate(@RequestBody @Valid PeriodRequest request, Out<PeriodRequest> body) throws InvalidPeriodException {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPeriodException("A name is required.");
        }
        LocalDate from = date(request.from(), "from-date");
        LocalDate to = date(request.to(), "to-date");
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The to-date must not be before the from-date.");
        }
        body.set(new PeriodRequest(name, from.toString(), to.toString()));
    }

    private static LocalDate date(String value, String label) throws InvalidPeriodException {
        try {
            return LocalDate.parse(value == null ? "" : value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + label + " must be yyyy-mm-dd.");
        }
    }
}
