package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.regex.Pattern;
import net.officefloor.hq.app.grade.Grades;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRate {
    private static final Pattern PCT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheet") String sheet, @RequestBody RateRequest request, Grades grades,
            Out<RateInput> body) throws InvalidRateException {
        body.set(check(request, grades.all(sheet)));
    }

    /** Checks a rate by the form's rules, as the rate import checks each of its lines. */
    static RateInput check(RateRequest request, Collection<String> grades) throws InvalidRateException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String pct = request == null || request.pct() == null ? "" : request.pct().trim();
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String to = request == null || request.to() == null ? "" : request.to().trim();
        if (!grades.contains(grade)) {
            throw new InvalidRateException("Grade must be one of the sheet's grades");
        }
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidRateException("The rate must be a percent of 0.00 or more with up to two decimals");
        }
        LocalDate on;
        try {
            on = from.isEmpty() ? null : LocalDate.parse(from);
        } catch (DateTimeParseException e) {
            throw new InvalidRateException("Not a valid from-date: " + from);
        }
        LocalDate through;
        try {
            through = to.isEmpty() ? null : LocalDate.parse(to);
        } catch (DateTimeParseException e) {
            throw new InvalidRateException("Not a valid to-date: " + to);
        }
        if (on != null && through != null && through.isBefore(on)) {
            throw new InvalidRateException("The to-date must not be before the from-date");
        }
        return new RateInput(grade, new BigDecimal(pct), on, through);
    }
}
