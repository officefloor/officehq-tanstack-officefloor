package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: a re-dating needs a real date (yyyy-MM-dd), the claim's new date. */
public class ValidateRedate {
    public void validate(@RequestBody RedateRequest request, Out<ClaimRedating> redating)
            throws InvalidClaimStatusException {
        String date = request == null || request.date() == null ? "" : request.date().trim();
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidClaimStatusException("A re-dating needs the claim's new date");
        }
        redating.set(new ClaimRedating(parsed, request.by() == null ? "" : request.by().trim(), null));
    }
}
