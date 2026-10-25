package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A person's fields: a name (required) and a grade, one of A, B or C. Publishes them trimmed, with the user making the change. */
public class ValidatePerson {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> person,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String name = PersonName.of(request == null ? null : request.name());
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new BadRequestException("Choose a grade: A, B or C");
        }
        person.set(new PersonRequest(name, grade, by.name(), request.version()));
        changedBy.set(by);
    }
}
