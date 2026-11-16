package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: undoing a transfer names the signed-in user. */
public class ValidateTransferUndo {
    public void validate(@RequestBody TransferUndoRequest request, Out<TransferUndoRequest> body) throws InvalidPersonException {
        String by = request == null || request.by() == null ? "" : request.by().trim();
        if (by.isEmpty()) {
            throw new InvalidPersonException("The user undoing the transfer is required");
        }
        body.set(new TransferUndoRequest(by));
    }
}
