package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the patron's id is required. */
public class ValidatePatron {
    public void validate(@RequestBody PatronRequest request, Out<PatronAdd> add) throws InvalidPersonException {
        if (request == null || request.patronId() == null) {
            throw new InvalidPersonException("The patron is required");
        }
        add.set(new PatronAdd(request.patronId(), request.by() == null ? "" : request.by().trim()));
    }
}
