package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/** Reads whether to start (true) or stop (false) watching; refuses a body without it, or no signed-in name. */
public class ValidateWatch {

    public record WatchRequest(Boolean on) {
    }

    public void validate(@RequestBody(required = false) WatchRequest request,
            @RequestHeader(name = "X-User", required = false) String user, Out<WatchInput> body)
            throws InvalidPersonException {
        if (request == null || request.on() == null) {
            throw new InvalidPersonException("Say whether to watch.");
        }
        String name = AuditUser.decode(user);
        if (name.isEmpty()) {
            throw new InvalidPersonException("Sign in to watch.");
        }
        body.set(new WatchInput(request.on(), name));
    }
}
