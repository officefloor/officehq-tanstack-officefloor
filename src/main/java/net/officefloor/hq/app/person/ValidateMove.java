package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A move's direction (up or down). Publishes it, with the user making the change. */
public class ValidateMove {
    public void validate(@RequestBody MoveRequest request, Out<MoveInput> move, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String direction = request == null || request.direction() == null ? "" : request.direction().trim();
        if (!direction.equals("up") && !direction.equals("down")) {
            throw new BadRequestException("Move a person up or down");
        }
        move.set(new MoveInput(direction.equals("up")));
        changedBy.set(by);
    }
}
