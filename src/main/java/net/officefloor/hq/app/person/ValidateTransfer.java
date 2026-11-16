package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: a transfer names the sheet the person goes to and the signed-in user. */
public class ValidateTransfer {
    public void validate(@RequestBody TransferRequest request, Out<TransferRequest> body) throws InvalidPersonException {
        String to = request == null || request.toSheet() == null ? "" : request.toSheet().trim();
        if (to.isEmpty()) {
            throw new InvalidPersonException("A transfer needs the sheet transferred to");
        }
        String by = request.by() == null ? "" : request.by().trim();
        if (by.isEmpty()) {
            throw new InvalidPersonException("The user transferring is required");
        }
        body.set(new TransferRequest(to, by));
    }
}
