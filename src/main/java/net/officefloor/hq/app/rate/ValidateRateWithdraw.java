package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the rate withdrawal from the body; refuses a from-date that is not a calendar date. */
public class ValidateRateWithdraw {

    public void validate(@RequestBody RateWithdrawRequest request, Out<RateWithdrawal> body) throws InvalidRateException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String from = request == null || request.from() == null ? "" : request.from().trim();
        LocalDate parsed = null;
        if (!from.isEmpty()) {
            try {
                parsed = LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The from-date must be a calendar date.");
            }
        }
        body.set(new RateWithdrawal(grade, parsed));
    }
}
