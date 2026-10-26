package net.officefloor.hq.app.session;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** The user whose locale is read or chosen: the signed-in name, trimmed. */
public class ValidateLocaleUser {
    public void validate(@PathVariable("user") String user, Out<UserResponse> out) throws BadRequestException {
        String name = user == null ? "" : user.trim();
        if (name.isEmpty() || name.length() > 200) {
            throw new BadRequestException("Sign in to choose a locale");
        }
        out.set(new UserResponse(name));
    }
}
