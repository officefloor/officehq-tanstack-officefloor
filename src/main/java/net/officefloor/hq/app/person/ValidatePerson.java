package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the person form: a name is required (trimmed), the grade must be A, B or C. */
public class ValidatePerson {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> person) throws BadRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("name is required");
        }
        if (!GRADES.contains(grade)) {
            throw new BadRequestException("grade must be A, B or C");
        }
        person.set(new PersonRequest(name, grade));
    }
}
