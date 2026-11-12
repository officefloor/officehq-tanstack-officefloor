package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SavePool {
    public void save(@PathVariable("sheetId") String sheetId, @Val PoolInput input, PoolStore store) {
        store.add(sheetId.trim(), input.name());
    }
}
