package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the move: a direction other than up or down is refused. */
public class ValidateMove {
    public void validate(@RequestBody MoveRequest request, Out<MoveRequest> body) throws InvalidPersonException {
        if (request == null || !("up".equals(request.direction()) || "down".equals(request.direction()))) {
            throw new InvalidPersonException("Move up or down?");
        }
        body.set(request);
    }
}
