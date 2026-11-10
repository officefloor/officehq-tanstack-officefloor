package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A deferral needs a date to defer the claim until. */
public class ValidateDefer {
    public void validate(@RequestBody DeferRequest request, Out<Deferral> body) throws InvalidClaimException {
        String raw = request == null || request.until() == null ? "" : request.until().trim();
        try {
            body.set(new Deferral(LocalDate.parse(raw)));
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("A deferral needs a date to defer the claim until");
        }
    }
}
