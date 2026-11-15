package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the watch: a body without whether to start or stop is refused. */
public class ValidateWatch {
    public void validate(@RequestBody WatchRequest request, Out<WatchRequest> body)
            throws InvalidPersonException {
        if (request == null || request.on() == null) {
            throw new InvalidPersonException("Watch on or off?");
        }
        body.set(request);
    }
}
