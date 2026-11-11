package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A settle may name a date to settle the claim as of; blank (or no body) settles it as of today. */
public class ValidateSettle {
    public void validate(@RequestBody SettleRequest request, Out<Settlement> body)
            throws InvalidClaimException {
        String raw = request == null || request.on() == null ? "" : request.on().trim();
        if (raw.isEmpty()) {
            body.set(new Settlement(null));
            return;
        }
        try {
            body.set(new Settlement(LocalDate.parse(raw)));
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("A settle date is a date (yyyy-MM-dd)");
        }
    }
}
