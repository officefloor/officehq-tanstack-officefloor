package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the sheet to transfer to (trimmed): refused when none is named. */
public class ValidateTransfer {
    public void validate(@RequestBody TransferRequest request, Out<TransferRequest> body)
            throws InvalidPersonException {
        String to = request == null || request.to() == null ? "" : request.to().trim();
        if (to.isEmpty()) {
            throw new InvalidPersonException("No sheet to transfer to");
        }
        body.set(new TransferRequest(to));
    }
}
