package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the pool to the sheet; refuses a name already a pool's. */
public class ApplyPool {
    public void apply(@Val PoolRequest request, @Val Sheet sheet, PoolRepository repository, Out<Pool> poolOut)
            throws InvalidPoolException {
        if (repository.existsBySheetIdAndNameIgnoreCase(sheet.getId(), request.name())) {
            throw new InvalidPoolException("There is already a pool with that name.");
        }
        poolOut.set(repository.saveAndFlush(new Pool(sheet.getId(), request.name())));
    }
}
