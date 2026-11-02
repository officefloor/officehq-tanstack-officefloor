package net.officefloor.hq.app.session;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateSignIn {
    public void validate(@RequestBody SignInRequest request, Out<Session> session) throws BadRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("name is required");
        }
        session.set(new Session(name));
    }
}
