package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePool {

    // The name, trimmed, must not be blank, nor already a pool of the sheet.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PoolRequest body, Pools pools,
            Out<Pool> pool) throws PoolRefusedException {
        String sheet = sheetId.trim();
        String name = body == null || body.name() == null ? "" : body.name().trim();
        if (name.isEmpty()) {
            throw new PoolRefusedException("Give the pool a name.");
        }
        if (pools.has(sheet, name)) {
            throw new PoolRefusedException("\"" + name + "\" is already a pool.");
        }
        pool.set(new Pool(sheet, name));
    }
}
