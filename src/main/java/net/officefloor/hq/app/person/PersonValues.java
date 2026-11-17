package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** A person's values once validated, and who gives them; for an edit, the version it was made from (null for an add).
 *  {@code gradeFrom} is the date the grade is given from, null when left blank; {@code terms} the person's own terms, a
 *  whole number of days 0 or more, null when left blank; {@code termsFrom} the date the terms are given from, null when
 *  left blank (the beginning of time); {@code contact} the person's
 *  contact without surrounding spaces and lower-cased, null when left blank; {@code tags} the person's tags (see
 *  #tags). */
public record PersonValues(String name, String grade, String by, Long version, LocalDate gradeFrom, Integer terms, String contact,
        List<String> tags, LocalDate termsFrom) {
    public PersonValues(String name, String grade, String by, Long version, LocalDate gradeFrom, Integer terms, String contact,
            List<String> tags) {
        this(name, grade, by, version, gradeFrom, terms, contact, tags, null);
    }

    /** The request's values by the person form's rules (see ValidatePerson), the grade one of the sheet's grades. */
    public static PersonValues from(PersonRequest request, Collection<String> grades) throws InvalidPersonException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("A name is required");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!grades.contains(grade)) {
            throw new InvalidPersonException("Grade must be one of the sheet's grades");
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
        String termsOn = request.termsFrom() == null ? "" : request.termsFrom().trim();
        LocalDate termsFrom = null;
        if (!termsOn.isEmpty()) {
            try {
                termsFrom = LocalDate.parse(termsOn);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The terms' from-date must be a date");
            }
        }
        String contact = request.contact() == null ? "" : request.contact().trim().toLowerCase(java.util.Locale.ROOT);
        return new PersonValues(name, grade, request.by() == null ? "" : request.by().trim(), request.version(), gradeFrom,
                terms, contact.isEmpty() ? null : contact, tags(request.tags()), termsFrom);
    }

    /** Comma separated tags as kept: each trimmed and in lower case, blanks and repeats dropped, sorted. */
    public static List<String> tags(String typed) throws InvalidPersonException {
        if (typed == null) {
            return List.of();
        }
        List<String> tags = List.copyOf(new TreeSet<>(Arrays.stream(typed.split(",")).map((t) -> t.trim().toLowerCase(Locale.ROOT))
                .filter((t) -> !t.isEmpty()).toList()));
        if (String.join(",", tags).length() > 2000) {
            throw new InvalidPersonException("The tags are too long");
        }
        return tags;
    }
}
