package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class AddPool {
    public void add(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val PoolRequest request, SheetRepository sheets, PoolRepository pools, Out<PoolAdded> added) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        pools.add(id, request.name(), user);
        added.set(new PoolAdded(id, request.name(), user));
    }
}
