package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateTransfer {
    public void validate(@RequestBody TransferRequest request, Out<TransferRequest> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        if (request == null || request.to() == null || request.to().isBlank()) {
            throw new InvalidSheetRequestException("Enter the id of the sheet to transfer to");
        }
        body.set(request);
        actor.set(new Actor(request.user()));
    }
}
