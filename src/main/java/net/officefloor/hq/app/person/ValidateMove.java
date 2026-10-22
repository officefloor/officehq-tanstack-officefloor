package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, and a direction of up or
 *  down. */
public class ValidateMove {
    public void validate(@RequestBody MoveRequest request, Out<MoveInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before moving people.");
        }
        String direction = request.direction() == null ? "" : request.direction().trim();
        if (!direction.equals("up") && !direction.equals("down")) {
            throw new InvalidPersonException("A person moves up or down.");
        }
        body.set(new MoveInput(request.user().trim(), direction.equals("up")));
    }
}
