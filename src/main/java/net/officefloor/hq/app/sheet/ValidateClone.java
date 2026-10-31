package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClone {
    public void validate(@RequestBody CloneRequest request, Out<CloneRequest> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        if (request == null || request.to() == null || request.to().isBlank()) {
            throw new InvalidSheetRequestException("Enter the id of the sheet to clone to");
        }
        body.set(request);
        actor.set(new Actor(request.user()));
    }
}
