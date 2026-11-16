package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: on and the signed-in user are required. */
public class ValidateWatch {
    public void validate(@RequestBody WatchRequest request, Out<WatchChange> change) throws InvalidPersonException {
        if (request == null || request.on() == null) {
            throw new InvalidPersonException("Whether to watch is required");
        }
        String by = request.by() == null ? "" : request.by().trim();
        if (by.isEmpty()) {
            throw new InvalidPersonException("The user watching is required");
        }
        change.set(new WatchChange(request.on(), by));
    }
}
