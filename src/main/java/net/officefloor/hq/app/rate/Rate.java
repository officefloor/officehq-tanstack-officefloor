package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A grade's rate on a sheet: a percent with two decimals, in force from a date (null: from the beginning of time)
 * through a to-date (null: with no end). Its level is how far up the sheet's parents it was found: 0 on the sheet itself,
 * 1 on its parent, and so on. Its version counts how many times it has been set for its grade and from-date; it
 * is always the newest version, the one in force. A forward-only rate (saved with a from-date on or after the sheet's
 * today) carries {@code after}: it applies only to claims added after it (see {@link #forClaim}); null for one that
 * applies to every claim. {@code earlier} holds its older versions, newest first, for the claims it does not apply to. */
public record Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to, int level, int version,
        Long after, List<Rate> earlier) {

    public Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to) {
        this(sheetId, grade, rate, from, to, 0, 1, null, List.of());
    }

    public Rate(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to, int version, Long after) {
        this(sheetId, grade, rate, from, to, 0, version, after, List.of());
    }

    /** The rate as found the given number of levels up. */
    public Rate at(int level) {
        return new Rate(sheetId, grade, rate, from, to, level, version, after, earlier.stream().map(e -> e.at(level)).toList());
    }

    /** The rate with its older versions, newest first. */
    public Rate withEarlier(List<Rate> versions) {
        return new Rate(sheetId, grade, rate, from, to, level, version, after, versions);
    }

    /** Whether this version applies to a claim added at {@code added} (null: before every forward-only rate). */
    public boolean appliesTo(Long added) {
        return after == null || (added != null && added > after);
    }

    /**
     * The rates as they apply to a claim added at {@code added}: each rate whose newest version applies to it, else its
     * newest older version that does; a rate none of whose versions applies is left out.
     */
    public static List<Rate> forClaim(List<Rate> rates, Long added) {
        List<Rate> found = new ArrayList<>();
        for (Rate r : rates) {
            if (r.appliesTo(added)) {
                found.add(r);
                continue;
            }
            r.earlier().stream().filter(e -> e.appliesTo(added)).findFirst().ifPresent(found::add);
        }
        return found;
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

    /** The grade's rate in force on the date for a claim added at {@code added} (see {@link #forClaim}). */
    public static BigDecimal on(List<Rate> rates, String grade, LocalDate on, Long added) {
        return on(forClaim(rates, added), grade, on);
    }
}
