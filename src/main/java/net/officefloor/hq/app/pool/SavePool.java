package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** A name already on the sheet's pools is refused. */
public class SavePool {
    public void save(@Val PoolInput input, @Val Sheet sheet, PoolRepository repository, Out<Pool> poolOut)
            throws InvalidPoolException {
        if (repository.find(sheet.id(), input.name()) != null) {
            throw new InvalidPoolException(input.name() + " is already a pool");
        }
        poolOut.set(repository.save(sheet.id(), input.name()));
    }
}
