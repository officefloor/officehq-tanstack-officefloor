package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateMove {
    public void validate(@RequestBody MoveRequest request, Out<Move> body) throws InvalidSheetRequestException {
        String direction = request == null || request.direction() == null ? "" : request.direction().trim();
        if (!direction.equals("up") && !direction.equals("down"))
            throw new InvalidSheetRequestException("A move is up or down: " + direction);
        body.set(new Move(direction.equals("up"), request.user()));
    }
}
