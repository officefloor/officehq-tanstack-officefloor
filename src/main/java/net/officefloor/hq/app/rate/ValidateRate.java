package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the rate form: the grade must be A, B or C; the percent is required, 0.00 or more with up
 *  to two decimals (below 100000); a from-date is yyyy-mm-dd, and blank means from the beginning of time; a to-date is yyyy-mm-dd, not before the from-date, and blank means
 *  no end. */
public class ValidateRate {
    private static final Set<String> GRADES = Set.of("A", "B", "C");
    private static final Pattern PCT = Pattern.compile("\\d{1,5}(\\.\\d{1,2})?|\\.\\d{1,2}");

    public void validate(@RequestBody RateRequest request, Out<RateInput> rate) throws BadRequestException {
        rate.set(check(request));
    }

    /** Check a rate form by these rules: the rate it sets. */
    static RateInput check(RateRequest request) throws BadRequestException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String pct = request == null || request.pct() == null ? "" : request.pct().trim();
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String to = request == null || request.to() == null ? "" : request.to().trim();
        if (!GRADES.contains(grade)) {
            throw new BadRequestException("grade must be A, B or C");
        }
        if (pct.isEmpty()) {
            throw new BadRequestException("rate is required");
        }
        if (!PCT.matcher(pct).matches()) {
            throw new BadRequestException("rate must be a percent of 0.00 or more with up to two decimals");
        }
        LocalDate on = null;
        if (!from.isEmpty()) {
            try {
                on = LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("from-date must be yyyy-mm-dd");
            }
        }
        LocalDate until = null;
        if (!to.isEmpty()) {
            try {
                until = LocalDate.parse(to);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("to-date must be yyyy-mm-dd");
            }
        }
        if (until != null && on != null && until.isBefore(on)) {
            throw new BadRequestException("to-date must not be before the from-date");
        }
        return new RateInput(grade, new BigDecimal(pct).setScale(2), on, until);
    }
}
