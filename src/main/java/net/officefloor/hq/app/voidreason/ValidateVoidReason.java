package net.officefloor.hq.app.voidreason;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the void reason's text from the body, trimmed; refuses a blank one. */
public class ValidateVoidReason {
    public void validate(@RequestBody VoidReasonRequest request, Out<VoidReasonRequest> body) throws InvalidVoidReasonException {
        String value = request == null || request.reason() == null ? "" : request.reason().trim();
        if (value.isEmpty()) {
            throw new InvalidVoidReasonException("A void reason cannot be blank.");
        }
        body.set(new VoidReasonRequest(value));
    }
}
