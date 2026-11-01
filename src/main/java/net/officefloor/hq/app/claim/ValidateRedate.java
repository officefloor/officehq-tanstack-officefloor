package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the redate body once, refusing one that names no valid date. */
public class ValidateRedate {
    public void validate(@RequestBody RedateRequest request, Out<LocalDate> date)
            throws InvalidClaimStatusChangeException {
        if (request == null || request.date() == null || request.date().isBlank()) {
            throw new InvalidClaimStatusChangeException("A redate names the claim's new date");
        }
        try {
            date.set(LocalDate.parse(request.date().trim()));
        } catch (DateTimeParseException e) {
            throw new InvalidClaimStatusChangeException("A redate date is yyyy-mm-dd");
        }
    }
}
