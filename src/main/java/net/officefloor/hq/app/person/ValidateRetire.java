package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the retire body, refusing a from-date that is not a date. */
public class ValidateRetire {
    public void validate(@RequestBody RetireRequest request, Out<RetireInput> retireOut) throws InvalidPersonException {
        String from = request == null || request.from() == null ? "" : request.from().trim();
        if (from.isEmpty()) {
            retireOut.set(new RetireInput(null));
            return;
        }
        try {
            retireOut.set(new RetireInput(LocalDate.parse(from)));
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The retired-from date must be a date (yyyy-mm-dd).");
        }
    }
}
