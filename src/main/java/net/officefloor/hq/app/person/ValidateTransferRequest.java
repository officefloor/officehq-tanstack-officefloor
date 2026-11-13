package net.officefloor.hq.app.person;

import jakarta.validation.Valid;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a transfer that names no sheet to transfer to; publishes it with the sheet id trimmed. */
public class ValidateTransferRequest {
    public void validate(@RequestBody @Valid TransferRequest request, Out<TransferRequest> transfer)
            throws InvalidPersonException {
        if (request.to() == null || request.to().isBlank()) {
            throw new InvalidPersonException("Enter the sheet to transfer to.");
        }
        transfer.set(new TransferRequest(request.to().trim()));
    }
}
