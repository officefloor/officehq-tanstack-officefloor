package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the person form from the body; refuses an empty name or a grade other than A, B or C. */
public class ValidatePerson {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> body) throws InvalidPersonException {
        String name = request == null || request.name() == null ? "" : request.name();
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (name.trim().isEmpty()) {
            throw new InvalidPersonException("A name is required.");
        }
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("The grade must be A, B or C.");
        }
        body.set(new PersonRequest(name, grade));
    }
}
