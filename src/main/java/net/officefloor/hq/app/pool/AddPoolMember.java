package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a person or another pool to a pool, refusing one already in it. A pool member is refused when it is
 *  already a member of a pool, when it would contain the pool through any chain (itself included), or when the
 *  chain of pools would be more than three deep. */
public class AddPoolMember {

    /** The most pools a chain of pools may hold, top to bottom. */
    private static final int MAX_DEPTH = 3;

    public void add(@PathVariable("sheetId") String sheetId, @Val PoolMemberInput input, ServerHttpConnection connection, PoolRepository repository, Audit audit)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        String pool = input.pool();
        if (!repository.exists(id, pool)) {
            throw new BadRequestException("no such pool");
        }
        String name;
        if (input.memberPool() != null) {
            name = input.memberPool();
            if (!repository.exists(id, name)) {
                throw new BadRequestException("no such pool: " + name);
            }
            if (repository.parentOf(id, name) != null) {
                throw new BadRequestException(name + " is already a member of a pool");
            }
            if (repository.containsOrIs(id, name, pool)) {
                throw new BadRequestException(name + " would contain " + pool);
            }
            if (repository.depthAbove(id, pool) + repository.heightBelow(id, name) > MAX_DEPTH) {
                throw new BadRequestException("a chain of pools is at most " + MAX_DEPTH + " deep");
            }
            if (!repository.addPoolMember(id, pool, name, input.share())) {
                throw new BadRequestException(name + " is already a member of a pool");
            }
        } else {
            name = repository.personName(id, input.personId());
            if (name == null) {
                throw new BadRequestException("no such person");
            }
            if (!repository.addMember(id, pool, input.personId(), input.share())) {
                throw new BadRequestException(name + " is already in the pool");
            }
        }
        audit.record("POOL_MEMBER_ADDED sheet=" + id + " by=" + by + " pool=" + pool + " name=" + name + " share="
                + input.share().toPlainString());
    }
}
