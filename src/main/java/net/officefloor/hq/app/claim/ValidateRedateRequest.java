package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the re-date's body once, publishing its new date; refuses (400) a missing or malformed date. */
public class ValidateRedateRequest {
    public void validate(@RequestBody RedateRequest request, Out<NewClaimDate> date) throws InvalidClaimException {
        String typed = request == null || request.date() == null ? "" : request.date().trim();
        if (typed.isEmpty()) {
            throw new InvalidClaimException("Type the claim's new date.");
        }
        try {
            date.set(new NewClaimDate(LocalDate.parse(typed)));
        } catch (DateTimeParseException ex) {
            throw new InvalidClaimException("Type a date as yyyy-mm-dd.");
        }
    }
}
