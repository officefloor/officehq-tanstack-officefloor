package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateMove {
    public void validate(@RequestBody MoveRequest request, Out<MoveInput> body) throws InvalidPersonException {
        String direction = request == null || request.direction() == null ? "" : request.direction().trim();
        switch (direction) {
            case "up" -> body.set(new MoveInput(true));
            case "down" -> body.set(new MoveInput(false));
            default -> throw new InvalidPersonException("A move is up or down");
        }
    }
}
