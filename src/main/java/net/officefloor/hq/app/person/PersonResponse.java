package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A person as the screen sees it. {@code grade} is the grade in force on the sheet's today; {@code grades} are
 * all the grades they were given, by from-date (yyyy-mm-dd; null for the first, from the beginning of time).
 * {@code retiredFrom} is yyyy-mm-dd, null when not retired; {@code held} is whether they have any
 * hold; {@code holds} are their holds by from-date (yyyy-mm-dd; {@code to} null when it has no end).
 * {@code terms} are their own terms in days, null for none; {@code contact} is their contact, null for none;
 * {@code aliases} are their aliases, sorted as names are compared.
 */
public record PersonResponse(long id, String name, String grade, List<Graded> grades, long version,
        String retiredFrom, boolean held, List<Held> holds, Integer terms, String contact, List<String> aliases) {

    public record Graded(String from, String grade) {
    }

    public record Held(String from, String to) {
    }

    public static PersonResponse from(Person person, LocalDate today) {
        List<Graded> grades = new ArrayList<>();
        grades.add(new Graded(null, person.grade()));
        person.dated().forEach(d -> grades.add(new Graded(d.from().toString(), d.grade())));
        return new PersonResponse(person.id(), person.name(), person.gradeOn(today), grades, person.version(),
                person.retiredFrom() == null ? null : person.retiredFrom().toString(), person.held(),
                person.holds().stream().map(h -> new Held(h.from().toString(), h.to() == null ? null : h.to().toString()))
                        .toList(), person.terms(), person.contact(), person.aliases());
    }
}
