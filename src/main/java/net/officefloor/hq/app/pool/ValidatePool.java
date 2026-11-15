package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.crew.CrewRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for a blank name, and for one already used by a pool or a crew of the sheet (compared trimmed). */
public class ValidatePool {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PoolRequest request,
            PoolRepository pools, CrewRepository crews, Out<PoolRequest> body) throws InvalidPoolException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPoolException("A pool needs a name");
        }
        if (pools.contains(sheetId.trim(), name)) {
            throw new InvalidPoolException("Already a pool: " + name);
        }
        if (crews.contains(sheetId.trim(), name)) {
            throw new InvalidPoolException("Already a crew, and so a pool: " + name);
        }
        body.set(new PoolRequest(name));
    }
}
