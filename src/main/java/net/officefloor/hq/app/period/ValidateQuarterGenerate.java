package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a year yyyy. The year is
 *  passed on as the months January to December it runs. */
public class ValidateQuarterGenerate {
    public void validate(@RequestBody QuarterGenerateRequest request, Out<PeriodGenerateInput> body)
            throws InvalidPeriodException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPeriodException("Sign in before generating periods.");
        }
        String year = request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidPeriodException("The year must be a year, yyyy.");
        }
        int y = Integer.parseInt(year);
        body.set(new PeriodGenerateInput(request.user().trim(), YearMonth.of(y, 1), YearMonth.of(y, 12)));
    }
}
