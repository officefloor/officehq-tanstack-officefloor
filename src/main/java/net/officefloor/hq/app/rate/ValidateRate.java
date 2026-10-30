package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;

public class ValidateRate {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** A percent: 0 or more, at most seven whole digits and two decimals. */
    private static final Pattern PERCENT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RateRequest request, Out<ValidRate> body, Settings settings)
            throws InvalidRateException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        body.set(check(request));
    }

    /** The rate form's rules: a grade A, B or C, a percent, an optional from-date and an optional
     *  to-date not before it. */
    static ValidRate check(RateRequest request) throws InvalidRateException {
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidRateException("Grade must be A, B or C");
        }
        String raw = request.rate() == null ? "" : request.rate().trim();
        if (!PERCENT.matcher(raw).matches()) {
            throw new InvalidRateException("The rate must be a percent of 0.00 or more with at most two decimals");
        }
        String rawFrom = request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!rawFrom.isEmpty()) {
            try {
                from = LocalDate.parse(rawFrom, DATE);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The from-date must be a date (YYYY-MM-DD)");
            }
        }
        String rawTo = request.to() == null ? "" : request.to().trim();
        LocalDate to = null;
        if (!rawTo.isEmpty()) {
            try {
                to = LocalDate.parse(rawTo, DATE);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The to-date must be a date (YYYY-MM-DD)");
            }
            if (from != null && to.isBefore(from)) {
                throw new InvalidRateException("The to-date must not be before the from-date");
            }
        }
        return new ValidRate(grade, new BigDecimal(raw).setScale(2), from, to, request.user());
    }
}
