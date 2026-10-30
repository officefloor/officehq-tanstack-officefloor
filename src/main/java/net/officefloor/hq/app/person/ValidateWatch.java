package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateWatch {
    public void validate(@RequestBody WatchRequest request, Out<WatchRequest> body) throws InvalidPersonException {
        if (request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Only a signed-in user can watch a person");
        }
        body.set(request);
    }
}
