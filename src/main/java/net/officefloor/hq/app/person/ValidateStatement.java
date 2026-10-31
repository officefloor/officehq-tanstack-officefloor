package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateStatement {
    public void validate(@RequestBody StatementRequest request, Out<StatementRequest> body, Out<Actor> actor) {
        actor.set(new Actor(request != null ? request.user() : null));
        body.set(request != null ? request : new StatementRequest(null));
    }
}
