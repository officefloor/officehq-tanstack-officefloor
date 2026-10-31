package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<Actor> actor) {
        actor.set(new Actor(request != null ? request.user() : null));
    }
}
