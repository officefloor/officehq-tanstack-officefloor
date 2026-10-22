package net.officefloor.hq.app.voidreason;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a reason that is not
 *  blank. */
public class ValidateVoidReason {
    public void validate(@RequestBody VoidReasonRequest request, Out<VoidReasonInput> body)
            throws InvalidVoidReasonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidVoidReasonException("Sign in before adding a void reason.");
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidVoidReasonException("Give the void reason.");
        }
        body.set(new VoidReasonInput(request.user().trim(), reason));
    }
}
