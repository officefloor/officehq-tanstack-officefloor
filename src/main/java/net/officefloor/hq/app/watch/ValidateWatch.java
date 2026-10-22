package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a watched flag. */
public class ValidateWatch {
    public void validate(@RequestBody WatchRequest request, Out<WatchInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before watching people.");
        }
        if (request.watched() == null) {
            throw new InvalidPersonException("Say whether to watch this person.");
        }
        body.set(new WatchInput(request.user().trim(), request.watched()));
    }
}
