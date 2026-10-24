package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the direction of a move from the body; refuses one other than up or down. */
public class ValidateMove {

    public record MoveRequest(String direction) {
    }

    public void validate(@RequestBody(required = false) MoveRequest request, Out<MoveInput> body)
            throws InvalidPersonException {
        String direction = request == null || request.direction() == null ? "" : request.direction().trim();
        switch (direction) {
            case "up" -> body.set(new MoveInput(true));
            case "down" -> body.set(new MoveInput(false));
            default -> throw new InvalidPersonException("Not a direction: " + direction);
        }
    }
}
