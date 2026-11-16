package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the pool on the sheet; a name already taken there is refused. */
public class AddPool {
    public void add(@PathVariable("sheetId") String sheetId, @Val PoolValues values, PoolRepository pools)
            throws InvalidPoolException {
        if (!pools.add(sheetId.trim(), values.name())) {
            throw new InvalidPoolException(values.name() + " is already a pool");
        }
    }
}
