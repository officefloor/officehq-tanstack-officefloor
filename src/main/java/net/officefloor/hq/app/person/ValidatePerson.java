package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {
    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> body)
            throws InvalidPersonException {
        body.set(PersonForm.check(request));
    }
}
