package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPools {
    public void load(@PathVariable("sheetId") String sheetId, ServerHttpConnection connection,
            PoolRepository repository, Out<Pools> pools) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        pools.set(new Pools(repository.list(id)));
    }
}
