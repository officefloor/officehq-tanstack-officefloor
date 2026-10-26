package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    // The name is kept without surrounding spaces; a blank one is refused.
    public void validate(@RequestBody PersonRequest body, Out<PersonRequest> request) throws InvalidPersonException {
        String name = body == null || body.name() == null ? "" : body.name().trim();
        String grade = body == null || body.grade() == null ? "" : body.grade().trim();
        if (name.isEmpty()) {
            throw new InvalidPersonException("Type a name.");
        }
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("The grade must be A, B or C.");
        }
        request.set(new PersonRequest(name, grade));
    }
}
