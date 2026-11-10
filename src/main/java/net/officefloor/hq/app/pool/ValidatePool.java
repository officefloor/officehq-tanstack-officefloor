package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A pool needs a name, kept trimmed. */
public class ValidatePool {
    public void validate(@RequestBody PoolRequest request, Out<PoolInput> body) throws InvalidPoolException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) throw new InvalidPoolException("A pool name is required");
        if (name.length() > 200) throw new InvalidPoolException("A pool name is at most 200 characters");
        body.set(new PoolInput(name));
    }
}
