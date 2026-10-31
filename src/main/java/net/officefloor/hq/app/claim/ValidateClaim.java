package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {
    public void validate(@RequestBody ClaimRequest request, Out<ClaimValues> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        ClaimValues values = ClaimRequest.checked(request);
        actor.set(new Actor(values.user()));
        body.set(values);
    }
}
