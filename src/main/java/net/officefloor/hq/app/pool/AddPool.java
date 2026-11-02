package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a pool (its name trimmed); refused for a blank name or one already a pool or a crew on the sheet. */
public class AddPool {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody PoolRequest request, Pools pools, Audit audit) throws InvalidPoolException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPoolException("A pool's name must not be blank");
        }
        if (pools.has(sheet, name)) {
            throw new InvalidPoolException(name + " is already a pool");
        }
        if (pools.isCrew(sheet, name)) {
            throw new InvalidPoolException(name + " is a crew");
        }
        pools.add(sheet, name);
        audit.record("POOL_ADDED sheet=" + sheet + " by=" + user + " name=" + name);
    }
}
