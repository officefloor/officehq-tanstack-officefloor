package net.officefloor.hq.app.session;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** The user whose columns are read or chosen: the signed-in name, trimmed. */
public class ValidateColumnsUser {
    public void validate(@PathVariable("user") String user, Out<UserResponse> out) throws BadRequestException {
        String name = user == null ? "" : user.trim();
        if (name.isEmpty() || name.length() > 200) {
            throw new BadRequestException("Sign in to choose columns");
        }
        out.set(new UserResponse(name));
    }
}
