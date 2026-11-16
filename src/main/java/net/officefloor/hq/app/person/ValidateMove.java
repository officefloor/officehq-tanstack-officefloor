package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: a direction of up or down and the signed-in user are required. */
public class ValidateMove {
    public void validate(@RequestBody MoveRequest request, Out<PersonMove> move) throws InvalidPersonException {
        String direction = request == null || request.direction() == null ? "" : request.direction().trim();
        if (!direction.equals("up") && !direction.equals("down")) {
            throw new InvalidPersonException("The direction must be up or down");
        }
        String by = request.by() == null ? "" : request.by().trim();
        if (by.isEmpty()) {
            throw new InvalidPersonException("The user moving is required");
        }
        move.set(new PersonMove(direction.equals("up"), by));
    }
}
