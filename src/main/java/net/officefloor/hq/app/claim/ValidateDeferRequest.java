package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the deferral's body once, publishing its date; refuses (400) a missing or malformed date. */
public class ValidateDeferRequest {
    public void validate(@RequestBody DeferRequest request, Out<Deferral> deferral) throws InvalidClaimException {
        String until = request == null || request.until() == null ? "" : request.until().trim();
        if (until.isEmpty()) {
            throw new InvalidClaimException("Type the date to defer the claim until.");
        }
        try {
            deferral.set(new Deferral(LocalDate.parse(until)));
        } catch (DateTimeParseException ex) {
            throw new InvalidClaimException("Type a date as yyyy-mm-dd.");
        }
    }
}
