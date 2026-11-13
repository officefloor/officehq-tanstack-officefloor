package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadPools {
    public void load(@Val Sheet sheet, PoolRepository repository, Out<Pools> pools) {
        pools.set(new Pools(repository.findAll(sheet.id())));
    }
}
