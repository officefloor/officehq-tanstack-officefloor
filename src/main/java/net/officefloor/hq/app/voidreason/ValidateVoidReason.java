package net.officefloor.hq.app.voidreason;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the reason, trimmed, is not blank. */
public class ValidateVoidReason {
    public void validate(@RequestBody VoidReasonRequest request, Out<VoidReasonValues> values)
            throws InvalidVoidReasonException {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidVoidReasonException("A void reason must not be blank");
        }
        values.set(new VoidReasonValues(reason, request.by() == null ? "" : request.by().trim()));
    }
}
