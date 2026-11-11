package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** A rate to withdraw, as the screen sends it: its grade and its from-date (yyyy-MM-dd; blank for from the beginning of time). */
public record WithdrawRateRequest(String grade, String from) {

    /** The request checked: a grade and a from-date (or null). */
    public static WithdrawnRate checked(WithdrawRateRequest request) throws InvalidRateException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (grade.isEmpty()) throw new InvalidRateException("A grade is required");
        String from = request.from() == null ? "" : request.from().trim();
        try {
            return new WithdrawnRate(grade, from.isEmpty() ? null : LocalDate.parse(from));
        } catch (DateTimeParseException e) {
            throw new InvalidRateException("A from-date is a date, yyyy-MM-dd");
        }
    }
}
