package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the hold body, refusing one that does not say whether to hold or release. */
public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<HoldInput> holdOut) throws InvalidPersonException {
        if (request == null || request.held() == null) {
            throw new InvalidPersonException("Say whether to hold or release the person.");
        }
        holdOut.set(new HoldInput(request.held()));
    }
}
