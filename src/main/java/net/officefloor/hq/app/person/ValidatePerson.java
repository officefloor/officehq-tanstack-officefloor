package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the name is required (kept without the spaces around it), the grade one of A, B or C. */
public class ValidatePerson {
    public void validate(@RequestBody PersonRequest request, Out<PersonValues> values) throws InvalidPersonException {
        values.set(PersonValues.from(request));
    }
}
