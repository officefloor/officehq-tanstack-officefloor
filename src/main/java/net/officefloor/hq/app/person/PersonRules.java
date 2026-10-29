package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Set;
import java.util.regex.Pattern;

/** The person form's rules, shared by the form and the people import. */
final class PersonRules {

    private PersonRules() {
    }

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final Pattern DAYS = Pattern.compile("\\d{1,4}");

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** The request as it is stored; the name is kept without surrounding spaces, a blank one is refused. */
    public static PersonRequest parse(PersonRequest body) throws InvalidPersonException {
        String name = body == null || body.name() == null ? "" : body.name().trim();
        String grade = body == null || body.grade() == null ? "" : body.grade().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("Type a name.");
        }
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("The grade must be A, B or C.");
        }
        // A blank from-date is kept null: the sheet's today, filled in when stored.
        String from = date(body.gradeFrom());
        String termsFrom = date(body.termsFrom());
        String terms = body.terms() == null ? "" : body.terms().trim();
        if (!terms.isEmpty() && !DAYS.matcher(terms).matches()) {
            throw new InvalidPersonException("The terms must be a whole number of days, 0 or more.");
        }
        // The contact is kept trimmed and lower-cased; a blank one is none.
        String contact = body.contact() == null ? "" : body.contact().trim().toLowerCase(java.util.Locale.ROOT);
        return new PersonRequest(name, grade, body.version(), from.isEmpty() ? null : from,
                terms.isEmpty() ? null : Integer.toString(Integer.parseInt(terms)), contact.isEmpty() ? null : contact, tags(body.tags()),
                termsFrom.isEmpty() ? null : termsFrom);
    }

    /** A from-date as typed, trimmed ("" when blank); one that is not a date is refused. */
    private static String date(String typed) throws InvalidPersonException {
        String from = typed == null ? "" : typed.trim();
        if (!from.isEmpty()) {
            try {
                LocalDate.parse(from, ISO);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a date (yyyy-mm-dd): " + from);
            }
        }
        return from;
    }

    /** Comma separated tags as a person keeps them: each trimmed and lower-cased, blanks and repeats dropped, sorted. */
    static String tags(String typed) {
        java.util.List<String> tags = typed == null ? java.util.List.of() : java.util.Arrays.stream(typed.split(","))
                .map(t -> t.trim().toLowerCase(java.util.Locale.ROOT)).filter(t -> !t.isEmpty()).distinct().sorted().toList();
        return tags.isEmpty() ? null : String.join(",", tags);
    }
}
