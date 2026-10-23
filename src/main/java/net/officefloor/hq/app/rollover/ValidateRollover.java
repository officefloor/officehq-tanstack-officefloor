package net.officefloor.hq.app.rollover;

import net.officefloor.hq.app.period.InvalidPeriodException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a year yyyy. */
public class ValidateRollover {
    public void validate(@RequestBody RolloverRequest request, Out<RolloverInput> body) throws InvalidPeriodException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPeriodException("Sign in before rolling the year over.");
        }
        String year = request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidPeriodException("The fiscal year must be a year, yyyy.");
        }
        body.set(new RolloverInput(request.user().trim(), Integer.parseInt(year)));
    }
}
