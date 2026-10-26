package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateWatch {
    public void validate(@RequestBody WatchRequest request, Out<WatchRequest> body) throws InvalidPersonException {
        if (request == null || request.watched() == null) {
            throw new InvalidPersonException("Say whether to watch.");
        }
        body.set(request);
    }
}
