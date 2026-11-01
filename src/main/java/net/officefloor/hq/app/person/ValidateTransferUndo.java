package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateTransferUndo {
    public void validate(@RequestBody TransferUndoRequest request, Out<TransferUndoRequest> body, Out<Actor> actor) {
        TransferUndoRequest given = request != null ? request : new TransferUndoRequest(null);
        body.set(given);
        actor.set(new Actor(given.user()));
    }
}
