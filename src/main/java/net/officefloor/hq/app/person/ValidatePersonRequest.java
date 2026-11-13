package net.officefloor.hq.app.person;

import jakarta.validation.Valid;
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
        body.set(new PersonRequest(name, grade, request.version()));
    }
}
