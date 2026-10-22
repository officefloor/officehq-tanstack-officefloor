package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a name is required, the grade
 *  is one of A, B or C, the grade's from-date (optional) a real yyyy-mm-dd date. Publishes the request with its name trimmed. */
public class ValidatePerson {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before changing people.");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("A name is required.");
        }
        if (name.length() > 200) {
            throw new InvalidPersonException("A name is at most 200 characters.");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("The grade must be A, B or C.");
        }
        String gradeFrom = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!gradeFrom.isEmpty()) {
            try {
                LocalDate.parse(gradeFrom);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The grade's from-date must be a date, yyyy-mm-dd.");
            }
        }
        body.set(new PersonRequest(request.user().trim(), name, grade, request.version(),
                gradeFrom.isEmpty() ? null : gradeFrom));
    }
}
