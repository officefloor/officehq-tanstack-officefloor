package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the defer body once, refusing one that names no valid date. */
public class ValidateDefer {
    public void validate(@RequestBody DeferRequest request, Out<LocalDate> until)
            throws InvalidClaimStatusChangeException {
        if (request == null || request.until() == null || request.until().isBlank()) {
            throw new InvalidClaimStatusChangeException("A defer names the date the claim is deferred until");
        }
        try {
            until.set(LocalDate.parse(request.until().trim()));
        } catch (DateTimeParseException e) {
            throw new InvalidClaimStatusChangeException("A defer date is yyyy-mm-dd");
        }
    }
}
