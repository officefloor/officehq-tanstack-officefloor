package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateWatch {
    public void validate(@RequestBody WatchRequest request, Out<WatchInput> body) throws InvalidPersonException {
        if (request == null || request.on() == null) throw new InvalidPersonException("A watch is on or off");
        body.set(new WatchInput(request.on()));
    }
}
