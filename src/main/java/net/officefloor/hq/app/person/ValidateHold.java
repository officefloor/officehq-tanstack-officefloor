package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: whether to hold or release is required. */
public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<Hold> hold) throws InvalidPersonException {
        if (request == null || request.held() == null) {
            throw new InvalidPersonException("Whether to hold or release is required");
        }
        hold.set(new Hold(request.held(), request.by() == null ? "" : request.by().trim()));
    }
}
