package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A person as the screen sees it. {@code grade} is the grade in force on the sheet's today; {@code grades} are
 * all the grades they were given, by from-date (yyyy-mm-dd; null for the first, from the beginning of time).
 * {@code retiredFrom} is yyyy-mm-dd, null when not retired; {@code held} is whether they are on hold.
 */
public record PersonResponse(long id, String name, String grade, List<Graded> grades, long version,
        String retiredFrom, boolean held) {

    public record Graded(String from, String grade) {
    }

    public static PersonResponse from(Person person, LocalDate today) {
        List<Graded> grades = new ArrayList<>();
        grades.add(new Graded(null, person.grade()));
        person.dated().forEach(d -> grades.add(new Graded(d.from().toString(), d.grade())));
        return new PersonResponse(person.id(), person.name(), person.gradeOn(today), grades, person.version(),
                person.retiredFrom() == null ? null : person.retiredFrom().toString(), person.held());
    }
}
