package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.setting.Settings;
import java.util.regex.Pattern;

/**
 * A rate as the screen sends it: a grade (one to three capital letters, one on the sheet), a percent with up to two decimals, and an
 * optional from-date (yyyy-MM-dd; blank for from the beginning of time) and an optional to-date
 * (yyyy-MM-dd; blank for no end).
 */
public record RateRequest(String grade, String pct, String from, String to) {
    private static final Pattern PCT = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");

    /** A rate request checked by the rate form's rules. */
    public static RateInput checked(RateRequest request) throws InvalidRateException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!Settings.isGradeCode(grade)) throw new InvalidRateException("A grade is one to three capital letters");
        String pct = request.pct() == null ? "" : request.pct().trim();
        if (pct.isEmpty()) throw new InvalidRateException("A rate is required");
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidRateException("A rate is a percent of 0.00 or more, with up to two decimals");
        }
        LocalDate from = date(request.from(), "A from-date is a date, yyyy-MM-dd");
        LocalDate to = date(request.to(), "A to-date is a date, yyyy-MM-dd");
        if (to != null && from != null && to.isBefore(from)) {
            throw new InvalidRateException("A to-date is on or after the from-date");
        }
        return new RateInput(grade, new BigDecimal(pct).setScale(2), from, to);
    }

    /** A date (yyyy-MM-dd), or null when blank. */
    private static LocalDate date(String raw, String message) throws InvalidRateException {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) return null;
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException e) {
            throw new InvalidRateException(message);
        }
    }
}
