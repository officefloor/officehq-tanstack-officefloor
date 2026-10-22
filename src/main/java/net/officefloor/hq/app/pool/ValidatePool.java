package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a name that is not blank. */
public class ValidatePool {
    public void validate(@RequestBody PoolRequest request, Out<PoolInput> body) throws InvalidPoolException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPoolException("Sign in before adding a pool.");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPoolException("Give the pool a name.");
        }
        body.set(new PoolInput(request.user().trim(), name));
    }
}
