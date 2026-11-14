package net.officefloor.hq.app.person;

import java.util.List;

/**
 * A person as the screen shows them; the last statement's due-total and date are null before the first, and
 * {@code retiredFrom} is null while they are not retired; {@code held} is whether they have any hold, {@code holds}
 * each hold, oldest first. {@code grade} is the grade in force on the sheet's today; {@code grades} every grade
 * recorded, each from its from-date. {@code terms} are the person's own terms in days, null when they have none;
 * {@code contact} their contact, null when they have none. {@code clashing} is whether another sheet of the sheet's
 * tree uses their name or any of their aliases as a name or alias. {@code origin} is the sheet they were transferred
 * from, null when they were not transferred here. {@code pastRetirements} are the retirements they were unretired
 * from, oldest first. {@code tags} their tags, sorted (none: empty). {@code statements} each statement still standing as
 * {@code <statement date> <due-total>}, oldest first.
 */
public record PersonResponse(long id, String name, String grade, int version, int statementCount, String statedTotal,
        String statementDate, boolean retired, String retiredFrom, boolean held,
        List<Grade> grades, List<HoldDates> holds, Integer terms, String contact,
        boolean clashing, String origin, List<RetirementDates> pastRetirements, List<String> tags,
        List<String> statements) {


    /** A grade recorded for the person, from its from-date (yyyy-mm-dd; null: from the beginning of time). */
    public record Grade(String grade, String from) {
    }

    /** A hold on the person, from its from-date to its to-date, both inclusive (yyyy-mm-dd; null: open-ended). */
    public record HoldDates(String from, String to) {
    }

    /** A retirement the person was unretired from, from its from-date to the day they were unretired (yyyy-mm-dd). */
    public record RetirementDates(String from, String to) {
    }

    public static PersonResponse from(Person person, boolean clashing, String origin, String tags,
            List<String> statements) {
        return new PersonResponse(person.id(), person.name(), person.grade(), person.version(), person.statementCount(),
                person.statedTotal(), person.statementDate() == null ? null : person.statementDate().toString(),
                person.retired(), person.retiredFrom() == null ? null : person.retiredFrom().toString(),
                person.held(), person.grades().stream()
                        .map((g) -> new Grade(g.grade(), g.from() == null ? null : g.from().toString())).toList(),
                person.holds().stream().map((h) -> new HoldDates(h.from() == null ? null : h.from().toString(),
                        h.to() == null ? null : h.to().toString())).toList(), person.terms(),
                person.contact(), clashing, origin, person.pastRetirements().stream()
                        .map((r) -> new RetirementDates(r.from().toString(), r.to().toString())).toList(),
                tags == null ? List.of() : List.of(tags.split(",")), statements);
    }
}
