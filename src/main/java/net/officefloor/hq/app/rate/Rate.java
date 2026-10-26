package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A grade's rate on a sheet: a percent with two decimals, in force from a date (null: from the beginning of time)
 * through a to-date (null: with no end). Its level is how far up the sheet's parents it was found: 0 on the sheet itself,
 * 1 on its parent, and so on. */
public record Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to, int level) {

    public Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to) {
        this(sheetId, grade, rate, from, to, 0);
    }

    /** The rate as found the given number of levels up. */
    public Rate at(int level) {
        return new Rate(sheetId, grade, rate, from, to, level);
    }

    /**
     * The grade's rate in force on the date among rates of several levels: the sheet's own, else (no rate of its own in
     * force then) its parent's, and so on up. On each level, the latest from-date on or before it, none counting as the
     * earliest; none when the date is after that rate's to-date.
     */
    public static BigDecimal on(java.util.List<Rate> rates, String grade, LocalDate on) {
        int top = rates.stream().mapToInt(Rate::level).max().orElse(0);
        for (int level = 0; level <= top; level++) {
            Rate found = null;
            for (Rate r : rates) {
                if (r.level() != level || !r.grade().equals(grade) || (r.from() != null && r.from().isAfter(on))) {
                    continue;
                }
                if (found == null || (r.from() != null && (found.from() == null || r.from().isAfter(found.from())))) {
                    found = r;
                }
            }
            if (found != null && (found.to() == null || !found.to().isBefore(on))) {
                return found.rate();
            }
        }
        return null;
    }
}
