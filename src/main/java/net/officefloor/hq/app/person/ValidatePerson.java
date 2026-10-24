package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the person form from the body, trimming the name; refuses an empty name or a grade other than
 * A, B or C, and a grade from-date that is not a calendar date, and terms that are not a whole number of days.
 */
public class ValidatePerson {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    private static final Pattern TERMS = Pattern.compile("\\d{1,5}");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> body) throws InvalidPersonException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("A name is required.");
        }
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("The grade must be A, B or C.");
        }
        String gradeFrom = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!gradeFrom.isEmpty()) {
            try {
                LocalDate.parse(gradeFrom);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a date: " + gradeFrom);
            }
        }
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && !TERMS.matcher(terms).matches()) {
            throw new InvalidPersonException("The terms must be a whole number of days, 0 or more, or blank.");
        }
        body.set(new PersonRequest(name, grade, request.version(), gradeFrom, terms));
    }
}
