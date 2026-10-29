package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {

    // The name is kept without surrounding spaces; a blank one is refused.
    public void validate(@RequestBody PersonRequest body, Out<PersonRequest> request) throws InvalidPersonException {
        request.set(PersonRules.parse(body));
    }
}
