package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the person form from the body, checked by {@link PersonRules#check}: the name trimmed, refusing an empty
 * name or a grade other than A, B or C, a grade from-date that is not a calendar date, and terms that are not a
 * whole number of days; the contact is trimmed and lower-cased.
 */
public class ValidatePerson {
    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> body) throws InvalidPersonException {
        if (request == null) {
            throw new InvalidPersonException("A name is required.");
        }
        body.set(PersonRules.check(request.name(), request.grade(), request.version(), request.gradeFrom(),
                request.terms(), request.contact(), request.tags()));
    }
}
