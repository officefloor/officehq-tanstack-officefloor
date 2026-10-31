package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGeneratePlans {
    public void validate(@RequestBody GeneratePlansRequest request, Out<Actor> actor) {
        actor.set(new Actor(request == null ? null : request.user()));
    }
}
