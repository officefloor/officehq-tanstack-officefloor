package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import net.officefloor.hq.app.Audit;

/** The rate form's rules and its RATE_SET line, shared by the form's set and the rate import: the grade is one of the
 *  sheet's grades, the rate a percent 0.00 or more with up to two decimals, the from-date and to-date (each optional) real
 *  yyyy-mm-dd dates, the to-date not before the from-date. */
public final class RateRules {
    private RateRules() {
    }

    /** The rate set for the grade checked by the rate form's rules: refused when any fails. */
    public static RateInput check(String user, String grade, RateRequest request, List<String> grades) throws InvalidRateException {
        String g = grade == null ? "" : grade.trim();
        if (!grades.contains(g)) {
            throw new InvalidRateException("The grade must be one of the sheet's grades: " + String.join(", ", grades) + ".");
        }
        String rate = request.rate() == null ? "" : request.rate().trim();
        if (!rate.matches("\\d{1,5}(\\.\\d{1,2})?")) {
            throw new InvalidRateException("The rate must be a percent, 0.00 to 99999.99, with up to two decimals.");
        }
        String fromText = request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!fromText.isEmpty()) {
            try {
                from = LocalDate.parse(fromText);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The from-date must be a date, yyyy-mm-dd.");
            }
        }
        String toText = request.to() == null ? "" : request.to().trim();
        LocalDate to = null;
        if (!toText.isEmpty()) {
            try {
                to = LocalDate.parse(toText);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The to-date must be a date, yyyy-mm-dd.");
            }
            if (from != null && to.isBefore(from)) {
                throw new InvalidRateException("The to-date must not be before the from-date.");
            }
        }
        return new RateInput(user, g, new BigDecimal(rate).setScale(2), from, to);
    }

    /** The RATE_SET line for the stored rate, set by the input's user. */
    public static void record(RateInput input, Rate rate, Audit audit) {
        audit.record("RATE_SET sheet=" + rate.sheetId() + " by=" + input.user()
                + " grade=" + rate.grade() + " rate=" + rate.pct().toPlainString()
                + (rate.from() != null ? " from=" + rate.from() : "")
                + (rate.to() != null ? " to=" + rate.to() : ""));
    }
}
