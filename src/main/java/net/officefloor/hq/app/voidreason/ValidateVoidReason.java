package net.officefloor.hq.app.voidreason;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A void reason needs text; it is kept trimmed. */
public class ValidateVoidReason {
    public void validate(@RequestBody VoidReasonRequest request, Out<VoidReasonInput> body)
            throws InvalidVoidReasonException {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) throw new InvalidVoidReasonException("A void reason is required");
        body.set(new VoidReasonInput(reason));
    }
}
