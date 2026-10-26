package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRate {

    private static final Set<String> GRADES = Set.of("A", "B", "C");
    private static final Pattern PERCENT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // The rate is kept with two decimals; a grade other than A, B or C, or a rate that is not a
    // percent of 0.00 or more with up to two decimals, is refused. A blank from-date means from the
    // beginning of time; one that is not a date (yyyy-mm-dd) is refused. A blank to-date means with no end; one
    // that is not a date, or is before the from-date, is refused.
    public void validate(@PathVariable("sheetId") String sheetId, @PathVariable("grade") String grade,
            @RequestBody RateRequest body, Out<Rate> rateOut) throws InvalidRateException {
        String g = grade == null ? "" : grade.trim();
        String typed = body == null || body.rate() == null ? "" : body.rate().trim();
        if (!GRADES.contains(g)) {
            throw new InvalidRateException("The grade must be A, B or C.");
        }
        if (!PERCENT.matcher(typed).matches()) {
            throw new InvalidRateException("The rate must be a percent of 0.00 or more, with up to two decimals.");
        }
        String typedFrom = body == null || body.from() == null ? "" : body.from().trim();
        LocalDate from = null;
        if (!typedFrom.isEmpty()) {
            try {
                from = LocalDate.parse(typedFrom, ISO);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The from-date must be a date (yyyy-mm-dd).");
            }
        }
        String typedTo = body == null || body.to() == null ? "" : body.to().trim();
        LocalDate to = null;
        if (!typedTo.isEmpty()) {
            try {
                to = LocalDate.parse(typedTo, ISO);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The to-date must be a date (yyyy-mm-dd).");
            }
            if (from != null && to.isBefore(from)) {
                throw new InvalidRateException("The to-date must not be before the from-date.");
            }
        }
        rateOut.set(new Rate(sheetId.trim(), g, new BigDecimal(typed).setScale(2, RoundingMode.UNNECESSARY), from, to));
    }
}
