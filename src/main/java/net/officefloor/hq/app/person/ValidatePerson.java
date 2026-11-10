package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> body)
            throws InvalidPersonException {
        String name = PersonRequest.checkName(request == null ? null : request.name());
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) throw new InvalidPersonException("A grade is one of A, B or C");
        body.set(new PersonRequest(name, grade, request.version()));
    }
}
