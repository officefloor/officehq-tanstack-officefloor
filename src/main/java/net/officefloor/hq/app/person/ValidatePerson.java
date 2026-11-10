package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> body)
            throws InvalidPersonException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) throw new InvalidPersonException("A name is required");
        if (name.length() > 200) throw new InvalidPersonException("A name is at most 200 characters");
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) throw new InvalidPersonException("A grade is one of A, B or C");
        body.set(new PersonRequest(name, grade, request.version()));
    }
}
