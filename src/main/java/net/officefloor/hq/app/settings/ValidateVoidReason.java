package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the void reason form: a reason, trimmed, that is not blank. */
public class ValidateVoidReason {
    public void validate(@RequestBody VoidReasonRequest request, Out<String> voidReason) throws BadRequestException {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new BadRequestException("reason is required");
        }
        voidReason.set(reason);
    }
}
