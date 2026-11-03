package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the pool form: a name is required (surrounding spaces removed). */
public class ValidatePool {
    public void validate(@RequestBody PoolRequest request, Out<PoolRequest> pool) throws BadRequestException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("name is required");
        }
        pool.set(new PoolRequest(name));
    }
}
