package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads from the body the id of the sheet to transfer the person to. */
public class ValidateTransfer {

    public record TransferRequest(String toSheet) {
    }

    public void validate(@RequestBody(required = false) TransferRequest request, Out<TransferInput> body)
            throws InvalidPersonException {
        String to = request == null || request.toSheet() == null ? "" : request.toSheet().trim();
        if (to.isEmpty()) {
            throw new InvalidPersonException("Enter the id of the sheet to transfer to.");
        }
        if (to.length() > 200) {
            throw new InvalidPersonException("A sheet id is at most 200 characters.");
        }
        body.set(new TransferInput(to));
    }
}
