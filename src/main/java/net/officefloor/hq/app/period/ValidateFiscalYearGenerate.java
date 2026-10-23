package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a year yyyy. */
public class ValidateFiscalYearGenerate {
    public void validate(@RequestBody FiscalYearGenerateRequest request, Out<FiscalYearGenerateInput> body)
            throws InvalidPeriodException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPeriodException("Sign in before generating periods.");
        }
        String year = request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidPeriodException("The fiscal year must be a year, yyyy.");
        }
        body.set(new FiscalYearGenerateInput(request.user().trim(), Integer.parseInt(year)));
    }
}
