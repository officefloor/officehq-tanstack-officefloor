package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateMerge {
    public void validate(@RequestBody MergeRequest request, Out<MergeRequest> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        if (request == null || request.intoId() == null) {
            throw new InvalidSheetRequestException("Choose the person to merge into");
        }
        body.set(request);
        actor.set(new Actor(request.user()));
    }
}
