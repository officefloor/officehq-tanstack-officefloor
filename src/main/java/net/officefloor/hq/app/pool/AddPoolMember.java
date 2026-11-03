package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a person to a pool, refusing one already in it. */
public class AddPoolMember {
    public void add(@PathVariable("sheetId") String sheetId, @Val PoolMemberInput input, ServerHttpConnection connection, PoolRepository repository, Audit audit)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        String pool = input.pool();
        if (!repository.exists(id, pool)) {
            throw new BadRequestException("no such pool");
        }
        String name = repository.personName(id, input.personId());
        if (name == null) {
            throw new BadRequestException("no such person");
        }
        if (!repository.addMember(id, pool, input.personId(), input.share())) {
            throw new BadRequestException(name + " is already in the pool");
        }
        audit.record("POOL_MEMBER_ADDED sheet=" + id + " by=" + by + " pool=" + pool + " name=" + name + " share="
                + input.share().toPlainString());
    }
}
