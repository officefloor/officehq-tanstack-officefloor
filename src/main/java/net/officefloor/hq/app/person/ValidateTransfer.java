package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the transfer body once, refusing one that names no sheet. */
public class ValidateTransfer {
    public void validate(@RequestBody TransferRequest request, Out<TransferRequest> body) throws InvalidPersonException {
        if (request == null || request.to() == null || request.to().isBlank()) {
            throw new InvalidPersonException("A transfer names the sheet transferred to");
        }
        body.set(new TransferRequest(request.to().trim()));
    }
}
