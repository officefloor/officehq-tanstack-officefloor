package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** A person's dated grades, earliest first: each applies from its from-date (null: from the beginning of time). */
public record Grades(List<Grade> history) {

    /** A grade given from a from-date (null: from the beginning of time). */
    public record Grade(String grade, LocalDate from) {
        boolean appliesOn(LocalDate date) {
            return from == null || !from.isAfter(date);
        }
    }

    public static final Grades NONE = new Grades(List.of());

    /** The grade in force on the date: the one whose from-date is the latest on or before it; "" when none applies. */
    public String on(LocalDate date) {
        return history.stream().filter(g -> g.appliesOn(date))
                .max(Comparator.comparing(Grade::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Grade::grade).orElse("");
    }
}
