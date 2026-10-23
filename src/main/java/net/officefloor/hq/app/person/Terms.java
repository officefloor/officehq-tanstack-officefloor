package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** A person's own terms (days): those from the beginning of time (null: none), and those recorded from a from-date
 *  (each null: none), earliest first; and those in force on the sheet's today (or the as-of date) as the screen shows. */
public record Terms(Integer base, List<Dated> dated, Integer now) {

    /** Terms (null: none) recorded from a from-date. */
    public record Dated(LocalDate from, Integer days) {
    }

    /** Terms from the beginning of time only (null: none). */
    public static Terms of(Integer base) {
        return new Terms(base, List.of(), base);
    }

    /** The terms in force on the date: those with the latest from-date on or before it, else those from the
     *  beginning of time; null when none are in force (the grade's or the sheet's apply). */
    public Integer on(LocalDate date) {
        Dated latest = dated.stream().filter(d -> !d.from().isAfter(date)).max(Comparator.comparing(Dated::from))
                .orElse(null);
        return latest != null ? latest.days() : base;
    }

    /** These terms, showing those in force on the date. */
    public Terms shownOn(LocalDate date) {
        return new Terms(base, dated, on(date));
    }
}
