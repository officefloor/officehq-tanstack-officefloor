package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads from the body whether the person is to be held (true) or released (false). */
public class ValidateHold {

    public record HoldRequest(Boolean held) {
    }

    public void validate(@RequestBody(required = false) HoldRequest request, Out<HoldInput> body)
            throws InvalidPersonException {
        if (request == null || request.held() == null) {
            throw new InvalidPersonException("Say whether the person is held.");
        }
        body.set(new HoldInput(request.held()));
    }
}
