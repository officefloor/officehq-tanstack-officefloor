package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: a deferral needs a real date (yyyy-MM-dd) to defer the claim until. */
public class ValidateDefer {
    public void validate(@RequestBody DeferRequest request, Out<ClaimDeferral> deferral)
            throws InvalidClaimStatusException {
        String until = request == null || request.until() == null ? "" : request.until().trim();
        LocalDate date;
        try {
            date = LocalDate.parse(until);
        } catch (DateTimeParseException e) {
            throw new InvalidClaimStatusException("A deferral needs the date it defers the claim until");
        }
        deferral.set(new ClaimDeferral(date, request.by() == null ? "" : request.by().trim()));
    }
}
