package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateUnretire {
    public void validate(@RequestBody UnretireRequest request, Out<UnretireRequest> body, Out<Actor> actor) {
        UnretireRequest checked = request != null ? request : new UnretireRequest(null);
        body.set(checked);
        actor.set(new Actor(checked.user()));
    }
}
