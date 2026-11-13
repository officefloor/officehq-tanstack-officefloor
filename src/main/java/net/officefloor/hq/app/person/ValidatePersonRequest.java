package net.officefloor.hq.app.person;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePersonRequest {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody @Valid PersonRequest request, Out<PersonRequest> body)
            throws InvalidPersonException {
        // The name is kept without surrounding spaces; a blank one is refused.
        String name = request.name() == null ? "" : request.name().strip();
        if (name.isBlank()) {
            throw new InvalidPersonException("A name is required.");
        }
        String grade = request.grade() == null ? "" : request.grade().trim().toUpperCase();
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("The grade must be A, B or C.");
        }
        String from = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!from.isEmpty()) {
            try {
                LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The grade's from-date must be yyyy-mm-dd.");
            }
        }
        String terms = request.terms() == null ? "" : request.terms().trim();
        if (!terms.isEmpty() && !terms.matches("\\d{1,5}")) {
            throw new InvalidPersonException("The terms must be a whole number of days, 0 or more, or blank.");
        }
        body.set(new PersonRequest(name, grade, from, request.version(),
                terms.isEmpty() ? "" : String.valueOf(Integer.parseInt(terms))));
    }
}
