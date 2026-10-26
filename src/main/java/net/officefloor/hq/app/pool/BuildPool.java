package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Val;

/** Adds the pool; a name already on the sheet, as a pool or a crew, is refused. */
public class BuildPool {
    public void build(@Val Pool pool, PoolRepository repository) throws BadRequestException {
        if (repository.findId(pool.sheetId(), pool.name()) != null) {
            throw new BadRequestException("That pool is already on this sheet");
        }
        if (repository.isCrew(pool.sheetId(), pool.name())) {
            throw new BadRequestException("A crew of that name is already on this sheet");
        }
        repository.insert(pool.sheetId(), pool.name());
    }
}
