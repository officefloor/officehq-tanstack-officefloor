package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> body) throws InvalidPersonException {
        String name = request == null ? null : request.name();
        String grade = request == null ? null : request.grade();
        if (name == null || name.isBlank()) {
            throw new InvalidPersonException("A name is required.");
        }
        if (grade == null || !GRADES.contains(grade)) {
            throw new InvalidPersonException("The grade must be A, B or C.");
        }
        // Stored without surrounding spaces.
        body.set(new PersonRequest(name.strip(), grade));
    }
}
