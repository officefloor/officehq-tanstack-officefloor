package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/** The person form's own rules, shared by the form and the import box. */
public final class PersonForm {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    private PersonForm() {
    }

    /** The person form's rules: the request trimmed and normalised, or refused. */
    public static PersonRequest check(PersonRequest request) throws InvalidPersonException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("A name is required");
        }
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("Grade must be A, B or C");
        }
        String from = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!from.isEmpty()) {
            try {
                LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a date: " + from);
            }
        }
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty()) {
            if (!terms.matches("\\d{1,5}")) {
                throw new InvalidPersonException("Terms must be a whole number of days: " + terms);
            }
            terms = String.valueOf(Integer.parseInt(terms));
        }
        String contact = request.contact() == null ? "" : request.contact().trim().toLowerCase(java.util.Locale.ROOT);
        if (contact.length() > 200) {
            throw new InvalidPersonException("A contact is at most 200 characters");
        }
        return new PersonRequest(name, grade, from.isEmpty() ? null : from, terms.isEmpty() ? null : terms,
                request.version(), contact.isEmpty() ? null : contact,
                net.officefloor.hq.app.claim.ClaimForm.tags(request.tags()));
    }
}
