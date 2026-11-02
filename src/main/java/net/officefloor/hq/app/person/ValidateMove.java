package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The direction to move: up (towards the top of the list) or down. */
public class ValidateMove {
    public void validate(@RequestBody MoveRequest request, Out<Boolean> up) throws InvalidPersonException {
        String direction = request == null || request.direction() == null ? "" : request.direction().trim();
        switch (direction) {
            case "up" -> up.set(true);
            case "down" -> up.set(false);
            default -> throw new InvalidPersonException("A person moves up or down, not: " + direction);
        }
    }
}
