package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the pool's name from the body, trimmed; refuses a blank one. */
public class ValidatePool {
    public void validate(@RequestBody PoolRequest request, Out<PoolRequest> body) throws InvalidPoolException {
        String value = request == null || request.name() == null ? "" : request.name().trim();
        if (value.isEmpty()) {
            throw new InvalidPoolException("A pool's name cannot be blank.");
        }
        body.set(new PoolRequest(value));
    }
}
