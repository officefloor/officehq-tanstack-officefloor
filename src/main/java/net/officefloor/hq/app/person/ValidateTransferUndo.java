package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user. */
public class ValidateTransferUndo {
    public void validate(@RequestBody TransferUndoRequest request, Out<TransferUndoRequest> body)
            throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before undoing transfers.");
        }
        body.set(new TransferUndoRequest(request.user().trim()));
    }
}
