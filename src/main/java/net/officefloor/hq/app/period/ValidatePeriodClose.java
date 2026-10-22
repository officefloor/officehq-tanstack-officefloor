package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user. */
public class ValidatePeriodClose {
    public void validate(@RequestBody PeriodCloseRequest request, Out<PeriodCloseInput> body)
            throws InvalidPeriodException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPeriodException("Sign in before closing periods.");
        }
        body.set(new PeriodCloseInput(request.user().trim()));
    }
}
