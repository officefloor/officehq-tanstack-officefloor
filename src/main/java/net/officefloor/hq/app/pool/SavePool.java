package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a pool, refusing a name the sheet already has for a pool or a crew (a crew is also a pool). */
public class SavePool {
    public void save(@PathVariable("sheetId") String sheetId, @Val PoolRequest input, ServerHttpConnection connection,
            PoolRepository repository, Audit audit) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        if (repository.crewExists(id, input.name())) {
            throw new BadRequestException("a crew of that name already exists");
        }
        if (!repository.add(id, input.name())) {
            throw new BadRequestException("a pool of that name already exists");
        }
        audit.record("POOL_ADDED sheet=" + id + " by=" + by + " name=" + input.name());
    }
}
