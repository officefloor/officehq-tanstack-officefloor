package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> body) throws InvalidPersonException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (name.isBlank()) {
            throw new InvalidPersonException("A name is required");
        }
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("Grade must be A, B or C");
        }
        String from = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!from.isEmpty()) {
            try {
                from = LocalDate.parse(from).toString();
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a valid from-date: " + from);
            }
        }
        body.set(new PersonRequest(name, grade, request.version(), from.isEmpty() ? null : from));
    }
}
