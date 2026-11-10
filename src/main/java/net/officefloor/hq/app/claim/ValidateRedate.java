package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A re-dating needs the claim's new date. */
public class ValidateRedate {
    public void validate(@RequestBody RedateRequest request, Out<Redate> body) throws InvalidClaimException {
        String raw = request == null || request.date() == null ? "" : request.date().trim();
        try {
            body.set(new Redate(LocalDate.parse(raw)));
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("A re-dating needs the claim's new date");
        }
    }
}
