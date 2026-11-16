package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the pool's name, trimmed, is not blank. */
public class ValidatePool {
    public void validate(@RequestBody PoolRequest request, Out<PoolValues> values) throws InvalidPoolException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPoolException("A pool's name must not be blank");
        }
        values.set(new PoolValues(name, request.by() == null ? "" : request.by().trim()));
    }
}
