package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * A grade's rate on a sheet: a percent with two decimals, as the screen shows it, in force from its from-date
 * (yyyy-MM-dd; null when it applies from the beginning of time).
 */
public record Rate(String grade, String pct, String from) {

    /**
     * The percent the grade's rates give on a date: the one whose from-date is the latest on or before it, or null
     * when the date is before every rate the grade has.
     */
    public static String on(List<Rate> rates, String grade, LocalDate date) {
        return rates.stream().filter((r) -> r.grade().equals(grade))
                .filter((r) -> r.from() == null || !LocalDate.parse(r.from()).isAfter(date))
                .max(Comparator.comparing((Rate r) -> r.from() == null ? LocalDate.MIN : LocalDate.parse(r.from())))
                .map(Rate::pct).orElse(null);
    }
}
