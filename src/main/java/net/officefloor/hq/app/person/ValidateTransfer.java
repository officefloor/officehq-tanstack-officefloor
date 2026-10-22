package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a sheet to transfer to. */
public class ValidateTransfer {
    public void validate(@RequestBody TransferRequest request, Out<TransferInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before transferring people.");
        }
        if (request.toSheet() == null || request.toSheet().isBlank()) {
            throw new InvalidPersonException("Enter the id of the sheet to transfer to.");
        }
        body.set(new TransferInput(request.user().trim(), request.toSheet().trim()));
    }
}
