package net.officefloor.hq.app.rate;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRateRequest {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** 0.00 or more, with at most two decimals. */
    private static final Pattern PERCENT = Pattern.compile("\\d{1,5}(\\.\\d{1,2})?|\\.\\d{1,2}");

    public void validate(@RequestBody @Valid RateRequest request, Out<RateRequest> body) throws InvalidRateException {
        String grade = request.grade() == null ? "" : request.grade().trim().toUpperCase();
        if (!GRADES.contains(grade)) {
            throw new InvalidRateException("The grade must be A, B or C.");
        }
        String rate = request.rate() == null ? "" : request.rate().trim();
        if (rate.isEmpty()) {
            throw new InvalidRateException("A rate is required.");
        }
        if (!PERCENT.matcher(rate).matches()) {
            throw new InvalidRateException("The rate must be a percent of 0.00 or more, with at most two decimals.");
        }
        String from = request.from() == null ? "" : request.from().trim();
        if (!from.isEmpty()) {
            try {
                from = LocalDate.parse(from).toString();
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The from-date must be yyyy-mm-dd.");
            }
        }
        body.set(new RateRequest(grade, new BigDecimal(rate).setScale(2).toPlainString(), from.isEmpty() ? null : from));
    }
}
