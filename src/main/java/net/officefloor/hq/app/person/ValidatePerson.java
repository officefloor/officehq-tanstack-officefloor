package net.officefloor.hq.app.person;

import java.util.Set;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new InvalidSheetRequestException("A name is required");
        }
        if (request.grade() == null || !GRADES.contains(request.grade())) {
            throw new InvalidSheetRequestException("The grade must be A, B or C");
        }
        // Names are kept without the spaces around them.
        actor.set(new Actor(request.user()));
        body.set(new PersonRequest(request.name().trim(), request.grade(), request.user(), request.version()));
    }
}
