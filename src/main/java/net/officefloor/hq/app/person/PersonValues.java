package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/** A person's values once validated, and who gives them; for an edit, the version it was made from (null for an add).
 *  {@code gradeFrom} is the date the grade is given from, null when left blank; {@code terms} the person's own terms, a
 *  whole number of days 0 or more, null when left blank; {@code contact} the person's
 *  contact without surrounding spaces and lower-cased, null when left blank. */
public record PersonValues(String name, String grade, String by, Long version, LocalDate gradeFrom, Integer terms, String contact) {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** The request's values by the person form's rules (see ValidatePerson). */
    public static PersonValues from(PersonRequest request) throws InvalidPersonException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("A name is required");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("Grade must be A, B or C");
        }
        String from = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        LocalDate gradeFrom = null;
        if (!from.isEmpty()) {
            try {
                gradeFrom = LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The grade's from-date must be a date");
            }
        }
        String days = request.terms() == null ? "" : request.terms().trim();
        Integer terms = null;
        if (!days.isEmpty()) {
            if (!days.matches("[0-9]{1,6}")) {
                throw new InvalidPersonException("The terms must be a whole number of days, 0 or more");
            }
            terms = Integer.parseInt(days);
        }
        String contact = request.contact() == null ? "" : request.contact().trim().toLowerCase(java.util.Locale.ROOT);
        return new PersonValues(name, grade, request.by() == null ? "" : request.by().trim(), request.version(), gradeFrom,
                terms, contact.isEmpty() ? null : contact);
    }
}
