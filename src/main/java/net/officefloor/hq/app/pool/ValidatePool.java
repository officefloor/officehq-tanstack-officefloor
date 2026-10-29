package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.crew.Crews;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePool {

    // The name, trimmed, must not be blank, nor already a pool or a crew of the sheet.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PoolRequest body, Pools pools,
            Crews crews, Out<Pool> pool) throws PoolRefusedException {
        String sheet = sheetId.trim();
        String name = body == null || body.name() == null ? "" : body.name().trim();
        if (name.isEmpty()) {
            throw new PoolRefusedException("Give the pool a name.");
        }
        if (pools.has(sheet, name)) {
            throw new PoolRefusedException("\"" + name + "\" is already a pool.");
        }
        if (crews.has(sheet, name)) {
            throw new PoolRefusedException("\"" + name + "\" is already a crew.");
        }
        pool.set(new Pool(sheet, name));
    }
}
