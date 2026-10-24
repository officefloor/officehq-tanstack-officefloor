package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.Sheet;

/** The rate form's rules and how it sets a rate, shared by the form and the rate import. */
public final class RateRules {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    private static final Pattern PCT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private RateRules() {
    }

    /** Checks a rate as the rate form does, returning it validated. */
    public static RateInput check(RateRequest request) throws InvalidRateException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String pct = request == null || request.pct() == null ? "" : request.pct().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidRateException("The grade must be A, B or C.");
        }
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidRateException("The rate must be a percent, 0.00 or more, with up to two decimals.");
        }
        String from = request == null || request.from() == null ? "" : request.from().trim();
        LocalDate parsed = null;
        if (!from.isEmpty()) {
            try {
                parsed = LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The from-date must be a calendar date.");
            }
        }
        String to = request == null || request.to() == null ? "" : request.to().trim();
        LocalDate until = null;
        if (!to.isEmpty()) {
            try {
                until = LocalDate.parse(to);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The to-date must be a calendar date.");
            }
        }
        if (parsed != null && until != null && until.isBefore(parsed)) {
            throw new InvalidRateException("The to-date must not be before the from-date.");
        }
        return new RateInput(grade, new BigDecimal(pct).setScale(2), parsed, until);
    }

    /** Sets the grade's rate as the rate form does, returning it saved: when the grade already has one for the
     *  from-date, that one is kept as an older version and the new one is its next. One with a from-date on or after the
     *  sheet's today is forward-only: it applies only to claims added after it is saved. */
    public static Rate set(RateInput input, Sheet sheet, RateRepository repository, RateLogRepository log,
            RateVersionRepository versions) {
        Rate rate = repository.findWithFrom(sheet.getId(), input.grade(), input.from()).orElse(null);
        if (rate == null) {
            rate = new Rate(sheet.getId(), input.grade(), input.pct(), input.from(), input.to());
        } else {
            versions.saveAndFlush(new RateVersion(rate, sheet.getToday()));
            rate.nextVersion();
        }
        rate.setPct(input.pct());
        rate.setToDate(input.to());
        Rate saved = repository.saveAndFlush(rate);
        boolean forwardOnly = input.from() != null && !input.from().isBefore(sheet.getToday());
        log.saveAndFlush(new RateLog(saved, sheet.getToday(), forwardOnly ? log.lastClaimId() : null));
        return saved;
    }
}
