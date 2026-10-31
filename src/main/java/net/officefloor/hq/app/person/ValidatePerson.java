package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePerson {
    public void validate(@RequestBody PersonRequest request, Out<PersonRequest> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        PersonRequest checked = PersonRequest.checked(request);
        actor.set(new Actor(checked.user()));
        body.set(checked);
    }
}
